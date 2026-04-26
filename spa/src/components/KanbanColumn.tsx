import { t } from "../i18n";
import { useDroppable, useDraggable } from '@dnd-kit/core';
import { KanbanCard } from './KanbanCard';
import type { Card, Status } from '../types';

export function KanbanColumn({ status, cards, onCardClick }: {
  status: Status;
  cards: Card[];
  onCardClick: (id: number) => void;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: `col-${status.id}` });

  return (
    <div ref={setNodeRef}
      className={`flex-shrink-0 w-72 rounded-lg flex flex-col max-h-full transition-colors ${
        isOver ? 'bg-blue-50 ring-2 ring-blue-300' : 'bg-gray-50'
      }`}>
      <div className="px-3 py-2 font-semibold text-sm text-gray-700 border-b border-gray-200 flex items-center justify-between">
        <span>{status.name}</span>
        <span className="bg-gray-200 text-gray-600 text-xs px-2 py-0.5 rounded-full">{cards.length}</span>
      </div>
      <div className="flex-1 overflow-y-auto p-2">
        {cards.map((card) => (
          <DraggableCard key={card.id} card={card} onClick={() => onCardClick(card.id)} />
        ))}
        {cards.length === 0 && (
          <div className="text-center text-xs text-gray-400 py-8">{t("KanbanNoCards")}</div>
        )}
      </div>
    </div>
  );
}

function DraggableCard({ card, onClick }: { card: Card; onClick: () => void }) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({ id: card.id });

  const style = {
    transform: transform ? `translate(${transform.x}px, ${transform.y}px)` : undefined,
    opacity: isDragging ? 0.3 : 1,
  };

  return (
    <div ref={setNodeRef} style={style} {...listeners} {...attributes} onClick={onClick}>
      <KanbanCard card={card} />
    </div>
  );
}
