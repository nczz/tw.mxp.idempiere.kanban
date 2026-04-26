import { t } from '../i18n';
import { useState, useMemo } from 'react';
import {
  DndContext, DragOverlay, closestCorners, PointerSensor,
  useSensor, useSensors, type DragEndEvent,
} from '@dnd-kit/core';
import { KanbanColumn } from './KanbanColumn';
import { KanbanCard } from './KanbanCard';
import { useMoveCard } from '../hooks/useCards';
import type { Card, Status } from '../types';

type GroupBy = 'none' | 'project' | 'salesRep' | 'bpartner' | 'priority';

interface Props {
  statuses: Status[];
  cards: Card[];
  onError: (msg: string) => void;
  onCardClick: (id: number) => void;
  wipLimits?: Record<string, number>;
  groupBy?: GroupBy;
  priorityNames?: Record<string, string>;
}

function groupCards(cards: Card[], groupBy: GroupBy, priorityNames?: Record<string, string>): { key: string; label: string; cards: Card[] }[] {
  if (groupBy === 'none') return [{ key: '_all', label: '', cards }];
  const map = new Map<string, { label: string; cards: Card[] }>();
  const ungrouped = t('KanbanUngrouped');
  for (const c of cards) {
    let key: string, label: string;
    switch (groupBy) {
      case 'project':   key = String(c.projectId || 0);  label = c.projectName || ungrouped; break;
      case 'salesRep':  key = String(c.salesRepId || 0);  label = c.salesRepName || ungrouped; break;
      case 'bpartner':  key = String(c.bpartnerId || 0);  label = c.bpartnerName || ungrouped; break;
      case 'priority':  key = c.priority || '?';          label = priorityNames?.[c.priority] || c.priority || ungrouped; break;
    }
    if (!key || key === '0') { key = '_ungrouped'; label = ungrouped; }
    if (!map.has(key)) map.set(key, { label, cards: [] });
    map.get(key)!.cards.push(c);
  }
  return [...map.entries()]
    .sort(([a], [b]) => a === '_ungrouped' ? 1 : b === '_ungrouped' ? -1 : a.localeCompare(b))
    .map(([key, v]) => ({ key, label: v.label, cards: v.cards }));
}

/** A single swimlane row with its own DndContext (isolates drag within the row) */
function SwimlaneRow({ label, statuses, cards, allCards, onError, onCardClick, wipLimits }: {
  label: string; statuses: Status[]; cards: Card[]; allCards: Card[];
  onError: (msg: string) => void; onCardClick: (id: number) => void;
  wipLimits?: Record<string, number>;
}) {
  const moveCard = useMoveCard();
  const [activeCard, setActiveCard] = useState<Card | null>(null);
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

  // Global counts for WIP (across all swimlanes)
  const globalStatusCounts = useMemo(() => {
    const m = new Map<number, number>();
    for (const c of allCards) m.set(c.statusId, (m.get(c.statusId) || 0) + 1);
    return m;
  }, [allCards]);

  const cardsByStatus = useMemo(() => {
    const m = new Map<number, Card[]>();
    for (const s of statuses) m.set(s.id, []);
    for (const c of cards) { const l = m.get(c.statusId); if (l) l.push(c); }
    return m;
  }, [statuses, cards]);

  function handleDragEnd(event: DragEndEvent) {
    setActiveCard(null);
    const { active, over } = event;
    if (!over) return;
    const card = cards.find((c) => c.id === active.id);
    if (!card) return;
    let targetStatusId: number | undefined;
    const overId = String(over.id);
    if (overId.startsWith('col-')) targetStatusId = parseInt(overId.replace('col-', ''), 10);
    else { const oc = cards.find((c) => c.id === over.id); if (oc) targetStatusId = oc.statusId; }
    if (!targetStatusId || targetStatusId === card.statusId) return;

    const limit = wipLimits?.[String(targetStatusId)];
    if (limit && limit > 0 && (globalStatusCounts.get(targetStatusId) || 0) >= limit) {
      onError(t('KanbanWipExceeded')); return;
    }
    const targetStatus = statuses.find((s) => s.id === targetStatusId);
    if (targetStatus?.isFinalClose && !confirm(t('KanbanFinalCloseWarning'))) return;

    moveCard.mutate({ id: card.id, targetStatusId }, {
      onError: (err) => onError(`${t('KanbanMoveFailed')}: ${err.message}`),
    });
  }

  return (
    <DndContext sensors={sensors} collisionDetection={closestCorners}
      onDragStart={(e) => setActiveCard(cards.find((c) => c.id === e.active.id) || null)}
      onDragEnd={handleDragEnd}>
      <div className="mb-4">
        {label && (
          <div className="text-sm font-semibold text-gray-600 px-1 py-1 border-b border-gray-300 mb-2 sticky left-0">
            {label} <span className="text-gray-400 font-normal">({cards.length})</span>
          </div>
        )}
        <div className="flex gap-4 overflow-x-auto items-start">
          {statuses.map((status) => (
            <KanbanColumn key={status.id} status={status}
              cards={cardsByStatus.get(status.id) || []} onCardClick={onCardClick}
              wipLimit={wipLimits?.[String(status.id)]} />
          ))}
        </div>
      </div>
      <DragOverlay>{activeCard ? <KanbanCard card={activeCard} isDragging /> : null}</DragOverlay>
    </DndContext>
  );
}

export function KanbanBoard({ statuses, cards, onError, onCardClick, wipLimits, groupBy = 'none', priorityNames }: Props) {
  const groups = useMemo(() => groupCards(cards, groupBy, priorityNames), [cards, groupBy, priorityNames]);

  if (statuses.length === 0) {
    return <div className="flex items-center justify-center w-full h-full text-gray-400">{t("KanbanNoStatuses")}</div>;
  }

  return (
    <div className="overflow-auto p-4 h-full">
      {groups.map((g) => (
        <SwimlaneRow key={g.key} label={g.label} statuses={statuses}
          cards={g.cards} allCards={cards} onError={onError} onCardClick={onCardClick}
          wipLimits={wipLimits} />
      ))}
    </div>
  );
}
