import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { kanbanFetch } from '../api';
import type { Card, InitData } from '../types';

export function useInit() {
  return useQuery<InitData>({
    queryKey: ['init'],
    queryFn: () => kanbanFetch('/init').then((r) => r.json()),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCards(scope: string, requestTypeId?: number) {
  const params = new URLSearchParams({ scope, closed: 'false' });
  if (requestTypeId) params.set('requestTypeId', String(requestTypeId));

  return useQuery<{ cards: Card[] }>({
    queryKey: ['cards', scope, requestTypeId],
    queryFn: () => kanbanFetch(`/cards?${params}`).then((r) => r.json()),
  });
}

export function useMoveCard() {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: ({ id, targetStatusId }: { id: number; targetStatusId: number }) =>
      kanbanFetch(`/cards/${id}/move`, {
        method: 'POST',
        body: JSON.stringify({ targetStatusId }),
      }).then((r) => {
        if (!r.ok) throw new Error('Move failed');
        return r.json();
      }),

    onMutate: async ({ id, targetStatusId }) => {
      await qc.cancelQueries({ queryKey: ['cards'] });
      const prev = qc.getQueriesData<{ cards: Card[] }>({ queryKey: ['cards'] });

      // Optimistic update across all card queries
      qc.setQueriesData<{ cards: Card[] }>({ queryKey: ['cards'] }, (old) => {
        if (!old) return old;
        return {
          cards: old.cards.map((c) => (c.id === id ? { ...c, statusId: targetStatusId } : c)),
        };
      });

      return { prev };
    },

    onError: (_err, _vars, context) => {
      // Rollback all card queries
      context?.prev?.forEach(([key, data]) => {
        if (data) qc.setQueryData(key, data);
      });
    },

    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['cards'] });
    },
  });
}
