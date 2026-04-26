import { priorityHex, priorityLabel, dueColor } from '../utils/priority';
import { t } from '../i18n';
import type { Card } from '../types';

export function KanbanCard({ card, isDragging }: { card: Card; isDragging?: boolean }) {
  const dna = card.dateNextAction ? new Date(card.dateNextAction).toLocaleDateString() : '';
  const stale = getStaleInfo(card.lastMoveAt);

  return (
    <div className={`bg-white rounded-lg shadow p-3 mb-2 border transition-shadow ${
      isDragging ? 'shadow-lg ring-2 ring-blue-400 border-gray-200' :
      card.isEscalated ? 'border-red-400 ring-1 ring-red-200 hover:shadow-md cursor-grab active:cursor-grabbing' :
      stale.level === 'danger' ? 'border-red-300 hover:shadow-md cursor-grab active:cursor-grabbing' :
      stale.level === 'warn' ? 'border-yellow-300 hover:shadow-md cursor-grab active:cursor-grabbing' :
      'border-gray-200 hover:shadow-md cursor-grab active:cursor-grabbing'
    }`}>
      <div className="flex items-center justify-between mb-1">
        <span className="text-xs text-gray-400 font-mono">{card.documentNo}</span>
        <div className="flex items-center gap-1">
          {card.isEscalated && (
            <span className="text-xs bg-red-100 text-red-600 px-1 py-0.5 rounded">🚫</span>
          )}
          {stale.level !== 'ok' && (
            <span className={`text-xs px-1 py-0.5 rounded ${
              stale.level === 'danger' ? 'bg-red-100 text-red-600' : 'bg-yellow-100 text-yellow-700'
            }`} title={`${t('KanbanLastMoved')} ${stale.days}${t('KanbanDaysAgo')}`}>
              🕐 {stale.days}d
            </span>
          )}
          {card.priority && (
            <span className="text-xs text-white px-1.5 py-0.5 rounded"
              style={{ backgroundColor: priorityHex(card.priority) }}>
              {priorityLabel(card.priority)}
            </span>
          )}
        </div>
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

function getStaleInfo(lastMoveAt?: number): { level: 'ok' | 'warn' | 'danger'; days: number } {
  if (!lastMoveAt) return { level: 'ok', days: 0 };
  const days = Math.floor((Date.now() - lastMoveAt) / (1000 * 60 * 60 * 24));
  if (days >= 7) return { level: 'danger', days };
  if (days >= 3) return { level: 'warn', days };
  return { level: 'ok', days };
}
