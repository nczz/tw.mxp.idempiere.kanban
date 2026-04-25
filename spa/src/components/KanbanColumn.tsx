import { useSortable } from '@dnd-kit/react/sortable';
import { CollisionPriority } from '@dnd-kit/abstract';
import { KanbanCard } from './KanbanCard';
import type { Card, Status } from '../types';

export function KanbanColumn({ status, cards, index }: {
  status: Status;
  cards: Card[];
  index: number;
}) {
  const { ref } = useSortable({
    id: `col-${status.id}`,
    index,
    type: 'column',
    accept: ['item'],
    collisionPriority: CollisionPriority.Low,
  });

  return (
    <div ref={ref} className="flex-shrink-0 w-72 bg-gray-50 rounded-lg flex flex-col max-h-full">
      <div className="px-3 py-2 font-semibold text-sm text-gray-700 border-b border-gray-200 flex items-center justify-between">
        <span>{status.name}</span>
        <span className="bg-gray-200 text-gray-600 text-xs px-2 py-0.5 rounded-full">{cards.length}</span>
      </div>
      <div className="flex-1 overflow-y-auto p-2 space-y-0">
        {cards.map((card, i) => (
          <SortableCard key={card.id} card={card} index={i} columnId={status.id} />
        ))}
        {cards.length === 0 && (
          <div className="text-center text-xs text-gray-400 py-8">No cards</div>
        )}
      </div>
    </div>
  );
}

function SortableCard({ card, index, columnId }: { card: Card; index: number; columnId: number }) {
  const { ref } = useSortable({
    id: card.id,
    index,
    group: `col-${columnId}`,
    type: 'item',
    accept: 'item',
  });

  return (
    <div ref={ref}>
      <KanbanCard card={card} />
    </div>
  );
}
