import { priorityColor, priorityLabel, dueColor } from '../utils/priority';
import type { Card } from '../types';

export function KanbanCard({ card }: { card: Card }) {
  const dna = card.dateNextAction ? new Date(card.dateNextAction).toLocaleDateString() : '';

  return (
    <div className="bg-white rounded-lg shadow p-3 mb-2 cursor-grab active:cursor-grabbing border border-gray-200 hover:shadow-md transition-shadow">
      <div className="flex items-center justify-between mb-1">
        <span className="text-xs text-gray-400 font-mono">{card.documentNo}</span>
        {card.priority && (
          <span className={`text-xs text-white px-1.5 py-0.5 rounded ${priorityColor(card.priority)}`}>
            {priorityLabel(card.priority)}
          </span>
        )}
      </div>
      <div className="text-sm font-medium text-gray-800 mb-1 line-clamp-2">{card.summary}</div>
      {card.bpartnerName && (
        <div className="text-xs text-gray-500 mb-1 truncate">🏢 {card.bpartnerName}</div>
      )}
      {card.requestTypeName && (
        <span className="inline-block text-xs bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded mb-1">
          {card.requestTypeName}
        </span>
      )}
      <div className="flex items-center justify-between mt-2 text-xs text-gray-400">
        <span className="truncate max-w-[60%]">👤 {card.salesRepName}</span>
        {dna && <span className={dueColor(card.dueType)}>{dna}</span>}
      </div>
    </div>
  );
}
