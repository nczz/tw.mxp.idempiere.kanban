import { t } from '../i18n';
import { useDroppable, useDraggable } from '@dnd-kit/core';
import { KanbanCard } from './KanbanCard';
import type { Card, Status } from '../types';

export function KanbanColumn({ status, cards, onCardClick, wipLimit }: {
  status: Status;
  cards: Card[];
  onCardClick: (id: number) => void;
  wipLimit?: number;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: `col-${status.id}` });
  const count = cards.length;
  const atLimit = wipLimit != null && wipLimit > 0 && count >= wipLimit;
  const nearLimit = wipLimit != null && wipLimit > 0 && count >= wipLimit * 0.8;

  return (
    <div ref={setNodeRef}
      className={`flex-shrink-0 w-72 rounded-lg flex flex-col max-h-full transition-colors ${
        isOver && atLimit ? 'bg-red-50 ring-2 ring-red-300' :
        isOver ? 'bg-blue-50 ring-2 ring-blue-300' :
        atLimit ? 'bg-red-50' :
        nearLimit ? 'bg-yellow-50' :
        'bg-gray-50'
      }`}>
      <div className={`px-3 py-2 font-semibold text-sm border-b flex items-center justify-between ${
        atLimit ? 'text-red-700 border-red-200' : nearLimit ? 'text-yellow-700 border-yellow-200' : 'text-gray-700 border-gray-200'
      }`}>
        <span>{status.name}</span>
        <span className={`text-xs px-2 py-0.5 rounded-full ${
          atLimit ? 'bg-red-200 text-red-700' : nearLimit ? 'bg-yellow-200 text-yellow-700' : 'bg-gray-200 text-gray-600'
        }`}>
          {count}{wipLimit != null && wipLimit > 0 ? `/${wipLimit}` : ''}
        </span>
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
  const { attributes, listeners, setNodeRef: setDragRef, transform, isDragging } = useDraggable({ id: card.id });
  const { setNodeRef: setDropRef } = useDroppable({ id: card.id });
  const style = {
    transform: transform ? `translate(${transform.x}px, ${transform.y}px)` : undefined,
    opacity: isDragging ? 0.3 : 1,
  };
  return (
    <div ref={(node) => { setDragRef(node); setDropRef(node); }} style={style} {...listeners} {...attributes} onClick={onClick}>
      <KanbanCard card={card} />
    </div>
  );
}
