import { useState, useEffect, useCallback } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { KanbanBoard } from './components/KanbanBoard';
import { GanttView } from './components/GanttView';
import { ScopeFilter } from './components/ScopeFilter';
import { CardDetail } from './components/CardDetail';
import { NewCardDialog } from './components/NewCardDialog';
import { useInit, useCards } from './hooks/useCards';
import { hasToken } from './api';
import { setMessages, t } from './i18n';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
});

function KanbanApp() {
  const [scope, setScope] = useState('Private');
  const [requestTypeId, setRequestTypeId] = useState<number | undefined>();
  const [search, setSearch] = useState('');
  const [showClosed, setShowClosed] = useState(false);
  const [view, setView] = useState<'kanban' | 'gantt'>('kanban');
  const [selectedCardId, setSelectedCardId] = useState<number | null>(null);
  const [showNewCard, setShowNewCard] = useState(false);
  const [toast, setToast] = useState<{ msg: string; type: 'error' | 'info' } | null>(null);

  const { data: init, isLoading: initLoading, error: initError } = useInit();
  const { data: cardsData, isLoading: cardsLoading, error: cardsError } = useCards(scope, requestTypeId, search || undefined, showClosed);

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
          <div className="text-lg mb-2">⚠️ {t('KanbanNoToken')}</div>
          <div>{t('KanbanNoTokenHint')}</div>
        </div>
      </div>
    );
  }

  if (initLoading) return <div className="flex items-center justify-center h-screen text-gray-400">{t('KanbanLoading')}</div>;
  if (!init) return <div className="flex items-center justify-center h-screen text-red-500">{t('KanbanFailedToLoad')}</div>;

  // Initialize i18n with server translations
  if (init.messages) setMessages(init.messages as unknown as Record<string, string>);

  const statusCategoryId = requestTypeId
    ? init.requestTypes.find((rt) => rt.id === requestTypeId)?.statusCategoryId
    : undefined;
  const statuses = init.statuses.filter(
    (s) => s.isClosed === showClosed && (statusCategoryId == null || s.statusCategoryId === statusCategoryId)
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
        <button onClick={() => setShowClosed(!showClosed)}
          className={`text-xs px-3 py-1 rounded ${showClosed ? 'bg-gray-700 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}>
          {showClosed ? t('KanbanClosed') : t('KanbanOpen')}
        </button>
        <div className="flex gap-1 border rounded overflow-hidden">
          <button onClick={() => setView('kanban')}
            className={`text-xs px-2 py-1 ${view === 'kanban' ? 'bg-blue-500 text-white' : 'bg-white text-gray-600'}`}>
            {t('KanbanViewBoard')}
          </button>
          <button onClick={() => setView('gantt')}
            className={`text-xs px-2 py-1 ${view === 'gantt' ? 'bg-blue-500 text-white' : 'bg-white text-gray-600'}`}>
            {t('KanbanViewGantt')}
          </button>
        </div>
        <input
          type="text" placeholder={t('KanbanSearch')} value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="text-sm border border-gray-300 rounded px-2 py-1 w-48"
        />
        <button onClick={() => setShowNewCard(true)}
          className="text-sm bg-green-500 text-white px-3 py-1 rounded hover:bg-green-600">
          {t('KanbanNew')}
        </button>
      </div>

      {/* Board */}
      <div className="flex-1 overflow-hidden">
        {view === 'gantt' ? (
          <GanttView scope={scope} requestTypeId={requestTypeId} onCardClick={setSelectedCardId} />
        ) : cardsLoading ? (
          <div className="flex items-center justify-center h-full text-gray-400">{t('KanbanLoadingCards')}</div>
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
