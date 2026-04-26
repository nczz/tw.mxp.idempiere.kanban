import { useState } from 'react';
import {
  DndContext,
  DragOverlay,
  closestCorners,
  PointerSensor,
  useSensor,
  useSensors,
  type DragStartEvent,
  type DragEndEvent,
} from '@dnd-kit/core';
import { KanbanColumn } from './KanbanColumn';
import { KanbanCard } from './KanbanCard';
import { useMoveCard } from '../hooks/useCards';
import type { Card, Status } from '../types';

interface Props {
  statuses: Status[];
  cards: Card[];
  onError: (msg: string) => void;
}

export function KanbanBoard({ statuses, cards, onError }: Props) {
  const moveCard = useMoveCard();
  const [activeCard, setActiveCard] = useState<Card | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } })
  );

  // Group cards by statusId
  const cardsByStatus = new Map<number, Card[]>();
  for (const s of statuses) cardsByStatus.set(s.id, []);
  for (const c of cards) {
    const list = cardsByStatus.get(c.statusId);
    if (list) list.push(c);
  }

  function handleDragStart(event: DragStartEvent) {
    const card = cards.find((c) => c.id === event.active.id);
    setActiveCard(card || null);
  }

  function handleDragEnd(event: DragEndEvent) {
    setActiveCard(null);
    const { active, over } = event;
    if (!over) return;

    const cardId = active.id as number;
    const card = cards.find((c) => c.id === cardId);
    if (!card) return;

    // Determine target status: over could be a column or a card in a column
    let targetStatusId: number | undefined;
    const overId = String(over.id);

    if (overId.startsWith('col-')) {
      targetStatusId = parseInt(overId.replace('col-', ''), 10);
    } else {
      // Dropped on a card — find which column that card belongs to
      const overCard = cards.find((c) => c.id === over.id);
      if (overCard) targetStatusId = overCard.statusId;
    }

    if (!targetStatusId || targetStatusId === card.statusId) return;

    moveCard.mutate(
      { id: cardId, targetStatusId },
      { onError: (err) => onError(`Move failed: ${err.message}`) }
    );
  }

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCorners}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
    >
      <div className="flex gap-4 overflow-x-auto p-4 h-full items-start">
        {statuses.map((status) => (
          <KanbanColumn
            key={status.id}
            status={status}
            cards={cardsByStatus.get(status.id) || []}
          />
        ))}
        {statuses.length === 0 && (
          <div className="flex items-center justify-center w-full h-full text-gray-400">
            No statuses configured. Check Request Type settings.
          </div>
        )}
      </div>
      <DragOverlay>
        {activeCard ? <KanbanCard card={activeCard} isDragging /> : null}
      </DragOverlay>
    </DndContext>
  );
}
