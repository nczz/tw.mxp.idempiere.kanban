import { t } from '../i18n';
import { useState, useMemo } from 'react';
import {
  DndContext, DragOverlay, closestCorners, PointerSensor,
  useSensor, useSensors, type DragStartEvent, type DragEndEvent,
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
}

function groupCards(cards: Card[], groupBy: GroupBy): { key: string; label: string; cards: Card[] }[] {
  if (groupBy === 'none') return [{ key: '_all', label: '', cards }];
  const map = new Map<string, { label: string; cards: Card[] }>();
  const ungrouped = t('KanbanUngrouped');
  for (const c of cards) {
    let key: string, label: string;
    switch (groupBy) {
      case 'project':   key = String(c.projectId || 0);  label = c.projectName || ungrouped; break;
      case 'salesRep':  key = String(c.salesRepId || 0);  label = c.salesRepName || ungrouped; break;
      case 'bpartner':  key = String(c.bpartnerId || 0);  label = c.bpartnerName || ungrouped; break;
      case 'priority':  key = c.priority || '?';          label = c.priority || ungrouped; break;
    }
    if (!key || key === '0') { key = '_ungrouped'; label = ungrouped; }
    if (!map.has(key)) map.set(key, { label, cards: [] });
    map.get(key)!.cards.push(c);
  }
  // Sort: ungrouped last
  return [...map.entries()]
    .sort(([a], [b]) => a === '_ungrouped' ? 1 : b === '_ungrouped' ? -1 : a.localeCompare(b))
    .map(([key, v]) => ({ key, label: v.label, cards: v.cards }));
}

export function KanbanBoard({ statuses, cards, onError, onCardClick, wipLimits, groupBy = 'none' }: Props) {
  const moveCard = useMoveCard();
  const [activeCard, setActiveCard] = useState<Card | null>(null);
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));
  const groups = useMemo(() => groupCards(cards, groupBy), [cards, groupBy]);

  // Global cardsByStatus for WIP check
  const cardsByStatus = useMemo(() => {
    const m = new Map<number, Card[]>();
    for (const s of statuses) m.set(s.id, []);
    for (const c of cards) { const l = m.get(c.statusId); if (l) l.push(c); }
    return m;
  }, [statuses, cards]);

  function handleDragStart(event: DragStartEvent) {
    setActiveCard(cards.find((c) => c.id === event.active.id) || null);
  }

  function handleDragEnd(event: DragEndEvent) {
    setActiveCard(null);
    const { active, over } = event;
    if (!over) return;
    const card = cards.find((c) => c.id === active.id);
    if (!card) return;

    let targetStatusId: number | undefined;
    const overId = String(over.id);
    if (overId.startsWith('col-')) {
      targetStatusId = parseInt(overId.replace('col-', ''), 10);
    } else {
      const overCard = cards.find((c) => c.id === over.id);
      if (overCard) targetStatusId = overCard.statusId;
    }
    if (!targetStatusId || targetStatusId === card.statusId) return;

    const limit = wipLimits?.[String(targetStatusId)];
    if (limit && limit > 0) {
      const targetCount = (cardsByStatus.get(targetStatusId) || []).length;
      if (targetCount >= limit) { onError(t('KanbanWipExceeded')); return; }
    }

    const targetStatus = statuses.find((s) => s.id === targetStatusId);
    if (targetStatus?.isFinalClose && !confirm(t('KanbanFinalCloseWarning'))) return;

    moveCard.mutate({ id: card.id, targetStatusId }, {
      onError: (err) => onError(`${t('KanbanMoveFailed')}: ${err.message}`),
    });
  }

  if (statuses.length === 0) {
    return <div className="flex items-center justify-center w-full h-full text-gray-400">{t("KanbanNoStatuses")}</div>;
  }

  const isSwimlane = groupBy !== 'none';

  return (
    <DndContext sensors={sensors} collisionDetection={closestCorners}
      onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
      <div className={`overflow-auto p-4 h-full ${isSwimlane ? '' : 'flex gap-4 items-start'}`}>
        {isSwimlane ? (
          groups.map((g) => {
            const groupCardsByStatus = new Map<number, Card[]>();
            for (const s of statuses) groupCardsByStatus.set(s.id, []);
            for (const c of g.cards) { const l = groupCardsByStatus.get(c.statusId); if (l) l.push(c); }
            return (
              <div key={g.key} className="mb-4">
                <div className="text-sm font-semibold text-gray-600 px-1 py-1 border-b border-gray-300 mb-2 sticky left-0">
                  {g.label} <span className="text-gray-400 font-normal">({g.cards.length})</span>
                </div>
                <div className="flex gap-4 overflow-x-auto items-start">
                  {statuses.map((status) => (
                    <KanbanColumn key={`${g.key}-${status.id}`} status={status}
                      cards={groupCardsByStatus.get(status.id) || []} onCardClick={onCardClick}
                      wipLimit={wipLimits?.[String(status.id)]} />
                  ))}
                </div>
              </div>
            );
          })
        ) : (
          statuses.map((status) => (
            <KanbanColumn key={status.id} status={status}
              cards={cardsByStatus.get(status.id) || []} onCardClick={onCardClick}
              wipLimit={wipLimits?.[String(status.id)]} />
          ))
        )}
      </div>
      <DragOverlay>{activeCard ? <KanbanCard card={activeCard} isDragging /> : null}</DragOverlay>
    </DndContext>
  );
}
