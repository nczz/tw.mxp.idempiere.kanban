import { useQuery } from '@tanstack/react-query';
import { kanbanFetch } from '../api';
import { t } from '../i18n';

interface Metrics {
  cycleTime: { status: string; avgDays: number; count: number }[];
  throughput: { week: number; count: number }[];
}

export function MetricsView({ requestTypeId }: { requestTypeId?: number }) {
  const { data, isLoading } = useQuery<Metrics>({
    queryKey: ['metrics', requestTypeId],
    queryFn: () => kanbanFetch<Metrics>(`/metrics${requestTypeId ? `?requestType=${requestTypeId}` : ''}`),
  });

  if (isLoading) return <div className="flex items-center justify-center h-full text-gray-400">{t('KanbanLoading')}</div>;
  if (!data) return null;

  const maxCycle = Math.max(...data.cycleTime.map((c) => c.avgDays), 1);
  const maxTp = Math.max(...data.throughput.map((t) => t.count), 1);

  return (
    <div className="h-full overflow-auto p-6 space-y-8">
      {/* Cycle Time */}
      <div>
        <div className="text-sm font-semibold text-gray-700 mb-3">{t('KanbanCycleTime')}</div>
        {data.cycleTime.length === 0 ? (
          <div className="text-xs text-gray-400">{t('KanbanNoData')}</div>
        ) : (
          <div className="space-y-2">
            {data.cycleTime.map((c) => (
              <div key={c.status} className="flex items-center gap-2">
                <span className="text-xs text-gray-600 w-32 truncate">{c.status}</span>
                <div className="flex-1 bg-gray-100 rounded h-6 relative">
                  <div className="bg-blue-500 h-6 rounded text-xs text-white flex items-center px-2"
                    style={{ width: `${Math.max((c.avgDays / maxCycle) * 100, 8)}%` }}>
                    {c.avgDays}d
                  </div>
                </div>
                <span className="text-xs text-gray-400 w-16 text-right">{c.count} {t('KanbanWipCards')}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Throughput */}
      <div>
        <div className="text-sm font-semibold text-gray-700 mb-3">{t('KanbanThroughput')}</div>
        {data.throughput.length === 0 ? (
          <div className="text-xs text-gray-400">{t('KanbanNoData')}</div>
        ) : (
          <div className="flex items-end gap-1 h-40">
            {data.throughput.map((tp) => (
              <div key={tp.week} className="flex-1 flex flex-col items-center">
                <div className="bg-green-500 rounded-t w-full" style={{ height: `${(tp.count / maxTp) * 100}%`, minHeight: tp.count > 0 ? 8 : 0 }} />
                <span className="text-[9px] text-gray-400 mt-1">{new Date(tp.week).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</span>
                <span className="text-[10px] text-gray-600 font-medium">{tp.count}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
