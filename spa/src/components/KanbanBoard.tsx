import { useRef } from 'react';
import { DragDropProvider } from '@dnd-kit/react';
import { isSortable } from '@dnd-kit/react/sortable';
import { KanbanColumn } from './KanbanColumn';
import { useMoveCard } from '../hooks/useCards';
import type { Card, Status } from '../types';

interface Props {
  statuses: Status[];
  cards: Card[];
  onError: (msg: string) => void;
}

export function KanbanBoard({ statuses, cards, onError }: Props) {
  const moveCard = useMoveCard();
  const snapshot = useRef(cards);

  // Group cards by statusId
  const cardsByStatus = new Map<number, Card[]>();
  for (const s of statuses) cardsByStatus.set(s.id, []);
  for (const c of cards) {
    const list = cardsByStatus.get(c.statusId);
    if (list) list.push(c);
  }

  return (
    <DragDropProvider
      onDragStart={() => {
        snapshot.current = cards;
      }}
      onDragEnd={(event) => {
        if (event.canceled) return;

        const { source } = event.operation;

        // Handle both sortable and non-sortable sources
        let cardId: number | undefined;
        let targetStatusId: number | undefined;

        if (isSortable(source)) {
          const { initialGroup, group } = source;
          if (initialGroup == null || group == null || initialGroup === group) return;
          targetStatusId = parseInt(String(group).replace('col-', ''), 10);
          cardId = source.id as number;
        } else if (source && event.operation.target) {
          // Fallback: use source.id and target.id
          cardId = source.id as number;
          const targetId = String(event.operation.target.id);
          if (targetId.startsWith('col-')) {
            targetStatusId = parseInt(targetId.replace('col-', ''), 10);
          }
        }

        if (!cardId || !targetStatusId || isNaN(targetStatusId)) return;

        moveCard.mutate(
          { id: cardId, targetStatusId },
          {
            onError: (err) => {
              onError(`Move failed: ${err.message}`);
            },
          }
        );
      }}
    >
      <div className="flex gap-4 overflow-x-auto p-4 h-full items-start">
        {statuses.map((status, i) => (
          <KanbanColumn
            key={status.id}
            status={status}
            cards={cardsByStatus.get(status.id) || []}
            index={i}
          />
        ))}
        {statuses.length === 0 && (
          <div className="flex items-center justify-center w-full h-full text-gray-400">
            No statuses configured. Check Request Type settings.
          </div>
        )}
      </div>
    </DragDropProvider>
  );
}
