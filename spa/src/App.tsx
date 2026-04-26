import { useState, useEffect, useCallback } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { KanbanBoard } from './components/KanbanBoard';
import { ScopeFilter } from './components/ScopeFilter';
import { useInit, useCards } from './hooks/useCards';
import { hasToken } from './api';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
});

function KanbanApp() {
  const [scope, setScope] = useState('Private');
  const [requestTypeId, setRequestTypeId] = useState<number | undefined>();
  const [toast, setToast] = useState<{ msg: string; type: 'error' | 'info' } | null>(null);

  const { data: init, isLoading: initLoading, error: initError } = useInit();
  const { data: cardsData, isLoading: cardsLoading, error: cardsError } = useCards(scope, requestTypeId);

  const showToast = useCallback((msg: string, type: 'error' | 'info' = 'error') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 5000);
  }, []);

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

  // Show API errors
  useEffect(() => {
    if (initError) showToast(initError.message);
    if (cardsError) showToast(cardsError.message);
  }, [initError, cardsError, showToast]);

  // No token warning
  if (!hasToken()) {
    return (
      <div className="flex items-center justify-center h-screen text-gray-500 text-sm">
        <div className="text-center">
          <div className="text-lg mb-2">⚠️ No authentication token</div>
          <div>Please open this form from the iDempiere menu.</div>
        </div>
      </div>
    );
  }

  if (initLoading) return <div className="flex items-center justify-center h-screen text-gray-400">Loading...</div>;
  if (!init) return <div className="flex items-center justify-center h-screen text-red-500">Failed to load configuration</div>;

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
          <KanbanBoard statuses={statuses} cards={cardsData?.cards || []} onError={showToast} />
        )}
      </div>
      {toast && (
        <div className={`fixed bottom-4 right-4 px-4 py-2 rounded-lg shadow-lg text-sm text-white z-50 ${
          toast.type === 'error' ? 'bg-red-500' : 'bg-blue-500'
        }`}>
          {toast.msg}
        </div>
      )}
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
