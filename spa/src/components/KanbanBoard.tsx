import { useRef } from 'react';
import { DragDropProvider } from '@dnd-kit/react';
import { isSortable } from '@dnd-kit/react/sortable';
import { KanbanColumn } from './KanbanColumn';
import { useMoveCard } from '../hooks/useCards';
import type { Card, Status } from '../types';

export function KanbanBoard({ statuses, cards }: { statuses: Status[]; cards: Card[] }) {
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
        if (!isSortable(source)) return;

        const { initialGroup, group } = source;
        if (initialGroup == null || group == null || initialGroup === group) return;

        // Extract status ID from group string "col-123"
        const targetStatusId = parseInt(String(group).replace('col-', ''), 10);
        const cardId = source.id as number;

        moveCard.mutate({ id: cardId, targetStatusId });
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
      </div>
    </DragDropProvider>
  );
}
