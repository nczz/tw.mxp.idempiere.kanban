import { useState, useEffect, useCallback } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { KanbanBoard } from './components/KanbanBoard';
import { ScopeFilter } from './components/ScopeFilter';
import { CardDetail } from './components/CardDetail';
import { NewCardDialog } from './components/NewCardDialog';
import { useInit, useCards } from './hooks/useCards';
import { hasToken } from './api';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
});

function KanbanApp() {
  const [scope, setScope] = useState('Private');
  const [requestTypeId, setRequestTypeId] = useState<number | undefined>();
  const [search, setSearch] = useState('');
  const [selectedCardId, setSelectedCardId] = useState<number | null>(null);
  const [showNewCard, setShowNewCard] = useState(false);
  const [toast, setToast] = useState<{ msg: string; type: 'error' | 'info' } | null>(null);

  const { data: init, isLoading: initLoading, error: initError } = useInit();
  const { data: cardsData, isLoading: cardsLoading, error: cardsError } = useCards(scope, requestTypeId, search || undefined);

  const showToast = useCallback((msg: string, type: 'error' | 'info' = 'error') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 5000);
  }, []);

  useEffect(() => {
    const handler = (e: MessageEvent) => {
      if (e.data?.type === 'kanban-refresh') {
        queryClient.invalidateQueries({ queryKey: ['cards'] });
      }
    };
    window.addEventListener('message', handler);
    return () => window.removeEventListener('message', handler);
  }, []);

  useEffect(() => {
    if (initError) showToast(initError.message);
    if (cardsError) showToast(cardsError.message);
  }, [initError, cardsError, showToast]);

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
  if (!init) return <div className="flex items-center justify-center h-screen text-red-500">Failed to load</div>;

  const statusCategoryId = requestTypeId
    ? init.requestTypes.find((rt) => rt.id === requestTypeId)?.statusCategoryId
    : undefined;
  const statuses = init.statuses.filter(
    (s) => !s.isClosed && (statusCategoryId == null || s.statusCategoryId === statusCategoryId)
  );

  return (
    <div className="h-screen flex flex-col bg-gray-100">
      {/* Toolbar */}
      <div className="flex items-center gap-3 px-4 py-2 bg-white border-b border-gray-200">
        <ScopeFilter
          scope={scope} onScopeChange={setScope}
          requestTypes={init.requestTypes}
          requestTypeId={requestTypeId} onRequestTypeChange={setRequestTypeId}
        />
        <div className="flex-1" />
        <input
          type="text" placeholder="Search..." value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="text-sm border border-gray-300 rounded px-2 py-1 w-48"
        />
        <button onClick={() => setShowNewCard(true)}
          className="text-sm bg-green-500 text-white px-3 py-1 rounded hover:bg-green-600">
          + New
        </button>
      </div>

      {/* Board */}
      <div className="flex-1 overflow-hidden">
        {cardsLoading ? (
          <div className="flex items-center justify-center h-full text-gray-400">Loading cards...</div>
        ) : (
          <KanbanBoard
            statuses={statuses}
            cards={cardsData?.cards || []}
            onError={showToast}
            onCardClick={setSelectedCardId}
          />
        )}
      </div>

      {/* Modals */}
      {selectedCardId && (
        <CardDetail
          cardId={selectedCardId}
          init={init}
          onClose={() => setSelectedCardId(null)}
          onError={showToast}
        />
      )}
      {showNewCard && (
        <NewCardDialog
          init={init}
          onClose={() => setShowNewCard(false)}
          onError={showToast}
        />
      )}

      {/* Toast */}
      {toast && (
        <div className={`fixed bottom-4 right-4 px-4 py-2 rounded-lg shadow-lg text-sm text-white z-50 ${
          toast.type === 'error' ? 'bg-red-500' : 'bg-blue-500'
        }`}>{toast.msg}</div>
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
