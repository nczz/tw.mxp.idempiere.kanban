import { useState, useEffect } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { KanbanBoard } from './components/KanbanBoard';
import { ScopeFilter } from './components/ScopeFilter';
import { useInit, useCards } from './hooks/useCards';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
});

function KanbanApp() {
  const [scope, setScope] = useState('Private');
  const [requestTypeId, setRequestTypeId] = useState<number | undefined>();

  const { data: init, isLoading: initLoading } = useInit();
  const { data: cardsData, isLoading: cardsLoading } = useCards(scope, requestTypeId);

  // Listen for refresh push from ZK parent
  useEffect(() => {
    const handler = (e: MessageEvent) => {
      if (e.data?.type === 'kanban-refresh') {
        queryClient.invalidateQueries({ queryKey: ['cards'] });
      }
    };
    window.addEventListener('message', handler);
    return () => window.removeEventListener('message', handler);
  }, []);

  if (initLoading) return <div className="flex items-center justify-center h-screen text-gray-400">Loading...</div>;
  if (!init) return <div className="flex items-center justify-center h-screen text-red-500">Failed to load</div>;

  // Filter statuses: only open statuses for the selected request type
  const statusCategoryId = requestTypeId
    ? init.requestTypes.find((rt) => rt.id === requestTypeId)?.statusCategoryId
    : undefined;
  const statuses = init.statuses.filter(
    (s) => !s.isClosed && (statusCategoryId == null || s.statusCategoryId === statusCategoryId)
  );

  return (
    <div className="h-screen flex flex-col bg-gray-100">
      <ScopeFilter
        scope={scope}
        onScopeChange={setScope}
        requestTypes={init.requestTypes}
        requestTypeId={requestTypeId}
        onRequestTypeChange={setRequestTypeId}
      />
      <div className="flex-1 overflow-hidden">
        {cardsLoading ? (
          <div className="flex items-center justify-center h-full text-gray-400">Loading cards...</div>
        ) : (
          <KanbanBoard statuses={statuses} cards={cardsData?.cards || []} />
        )}
      </div>
    </div>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <KanbanApp />
    </QueryClientProvider>
  );
}
