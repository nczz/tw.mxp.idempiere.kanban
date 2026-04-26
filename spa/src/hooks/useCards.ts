import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { kanbanFetch } from '../api';
import type { Card, InitData } from '../types';

export function useInit() {
  return useQuery<InitData>({
    queryKey: ['init'],
    queryFn: () => kanbanFetch<InitData>('/init'),
    staleTime: 5 * 60 * 1000,
    retry: 2,
  });
}

export function useCards(scope: string, requestTypeId?: number, search?: string, closed?: boolean) {
  const params = new URLSearchParams({ scope, closed: closed ? 'true' : 'false' });
  if (requestTypeId) params.set('requestTypeId', String(requestTypeId));
  if (search) params.set('search', search);

  return useQuery<{ cards: Card[] }>({
    queryKey: ['cards', scope, requestTypeId, search, closed],
    queryFn: () => kanbanFetch<{ cards: Card[] }>(`/cards?${params}`),
    refetchInterval: 30000,
  });
}

export interface CardDetail extends Card {
  result: string;
  requestTypeId: number;
  isEscalated: boolean;
  startDate: number | null;
  endTime: number | null;
  closeDate: number | null;
  created: number | null;
  requesterId: number;
  requesterName: string;
  createdBy: number;
  creatorName: string;
  bpartnerId: number;
  productId?: number;
  orderId?: number;
  invoiceId?: number;
  paymentId?: number;
  projectId: number;
  campaignId?: number;
  assetId?: number;
  activityId?: number;
  productName?: string;
  orderName?: string;
  invoiceName?: string;
  paymentName?: string;
  projectName: string;
  campaignName?: string;
  assetName?: string;
  activityName?: string;
  moveHistory: { date: number; userName: string; fromStatus: string; toStatus: string; note: string }[];
  comments: { id: number; text: string; date: number; userId: number; userName: string }[];
  attachments?: { name: string; size: number }[];
  activity?: { type: string; date: number; userName: string; detail: string }[];
}

export function useCardDetail(cardId: number | null) {
  return useQuery<CardDetail>({
    queryKey: ['card', cardId],
    queryFn: () => kanbanFetch<CardDetail>(`/cards/${cardId}`),
    enabled: cardId != null && cardId > 0,
  });
}

export function useMoveCard() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, targetStatusId }: { id: number; targetStatusId: number }) =>
      kanbanFetch<{ success: boolean }>(`/cards/${id}/move`, {
        method: 'POST', body: JSON.stringify({ targetStatusId }),
      }),
    onMutate: async ({ id, targetStatusId }) => {
      await qc.cancelQueries({ queryKey: ['cards'] });
      const prev = qc.getQueriesData<{ cards: Card[] }>({ queryKey: ['cards'] });
      qc.setQueriesData<{ cards: Card[] }>({ queryKey: ['cards'] }, (old) =>
        old ? { cards: old.cards.map((c) => (c.id === id ? { ...c, statusId: targetStatusId } : c)) } : old
      );
      return { prev };
    },
    onError: (_e, _v, ctx) => { ctx?.prev?.forEach(([k, d]) => { if (d) qc.setQueryData(k, d); }); },
    onSettled: () => { qc.invalidateQueries({ queryKey: ['cards'] }); },
  });
}

export function useUpdateCard() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, ...data }: { id: number; [key: string]: unknown }) =>
      kanbanFetch<{ success: boolean }>(`/cards/${id}`, {
        method: 'PUT', body: JSON.stringify(data),
      }),
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['cards'] });
      qc.invalidateQueries({ queryKey: ['card'] });
    },
  });
}

export function useUploadAttachment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ cardId, name, data }: { cardId: number; name: string; data: string }) =>
      kanbanFetch<{ success: boolean }>(`/attachments/${cardId}`, {
        method: 'POST', body: JSON.stringify({ name, data }),
      }),
    onSettled: () => { qc.invalidateQueries({ queryKey: ['card'] }); },
  });
}

export function useDeleteAttachment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ cardId, name }: { cardId: number; name: string }) =>
      kanbanFetch<{ success: boolean }>(`/attachments/${cardId}/${encodeURIComponent(name)}`, {
        method: 'DELETE',
      }),
    onSettled: () => { qc.invalidateQueries({ queryKey: ['card'] }); },
  });
}

export function useAddComment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ cardId, text }: { cardId: number; text: string }) =>
      kanbanFetch<{ success: boolean }>(`/cards/${cardId}/comment`, {
        method: 'POST', body: JSON.stringify({ text }),
      }),
    onSettled: () => { qc.invalidateQueries({ queryKey: ['card'] }); },
  });
}

export function useCreateCard() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: { summary: string; result?: string; requestTypeId?: number; statusId?: number; bpartnerId?: number; productId?: number; projectId?: number; campaignId?: number; activityId?: number; priority?: number; salesRepId?: number; dateNextAction?: number }) =>
      kanbanFetch<{ success: boolean; id: number }>('/cards', {
        method: 'POST', body: JSON.stringify(data),
      }),
    onSettled: () => { qc.invalidateQueries({ queryKey: ['cards'] }); },
  });
}

export function useReorderCards() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (cardIds: number[]) =>
      kanbanFetch('/cards/reorder', { method: 'POST', body: JSON.stringify({ cardIds }) }),
    onSettled: () => { qc.invalidateQueries({ queryKey: ['cards'] }); },
  });
}
