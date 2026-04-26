import { useQuery } from '@tanstack/react-query';
import { kanbanFetch } from '../api';
import { priorityHex } from '../utils/priority';
import { t } from '../i18n';

interface GanttTask {
  id: number;
  documentNo: string;
  summary: string;
  startDate: number;
  endDate?: number;
  priority: string;
  salesRepName: string;
  statusName: string;
  isClosed: boolean;
}

export function GanttView({ scope, requestTypeId, onCardClick }: {
  scope: string;
  requestTypeId?: number;
  onCardClick: (id: number) => void;
}) {
  const params = new URLSearchParams({ scope });
  if (requestTypeId) params.set('requestTypeId', String(requestTypeId));

  const { data, isLoading } = useQuery<{ tasks: GanttTask[] }>({
    queryKey: ['gantt', scope, requestTypeId],
    queryFn: () => kanbanFetch<{ tasks: GanttTask[] }>(`/gantt?${params}`),
  });

  if (isLoading) return <div className="flex items-center justify-center h-full text-gray-400">{t('KanbanLoading')}</div>;

  const tasks = data?.tasks || [];
  if (tasks.length === 0) return <div className="flex items-center justify-center h-full text-gray-400">{t('KanbanNoCards')}</div>;

  // Calculate date range
  const now = Date.now();
  const allDates = tasks.flatMap((t) => [t.startDate, t.endDate || t.startDate + 86400000]);
  const minDate = Math.min(...allDates, now - 7 * 86400000);
  const maxDate = Math.max(...allDates, now + 30 * 86400000);
  const totalDays = Math.ceil((maxDate - minDate) / 86400000) + 1;
  const dayWidth = 32;

  // Generate day headers
  const days: Date[] = [];
  for (let i = 0; i < totalDays; i++) {
    days.push(new Date(minDate + i * 86400000));
  }

  const todayOffset = Math.floor((now - minDate) / 86400000);

  return (
    <div className="h-full overflow-auto p-4">
      <div className="relative" style={{ minWidth: totalDays * dayWidth + 200 }}>
        {/* Header */}
        <div className="flex sticky top-0 bg-white z-10 border-b">
          <div className="w-48 flex-shrink-0 text-xs font-semibold text-gray-500 p-1">{t('KanbanSummary')}</div>
          <div className="flex">
            {days.map((d, i) => {
              const isToday = i === todayOffset;
              const isWeekend = d.getDay() === 0 || d.getDay() === 6;
              return (
                <div key={i} className={`text-center text-[10px] border-l ${
                  isToday ? 'bg-blue-100 font-bold text-blue-700' : isWeekend ? 'bg-gray-50 text-gray-400' : 'text-gray-500'
                }`} style={{ width: dayWidth }}>
                  {d.getDate() === 1 || i === 0 ? <div>{d.toLocaleDateString(undefined, { month: 'short' })}</div> : null}
                  <div>{d.getDate()}</div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Tasks */}
        {tasks.map((task) => {
          const startDay = Math.max(0, Math.floor((task.startDate - minDate) / 86400000));
          const endDay = task.endDate
            ? Math.floor((task.endDate - minDate) / 86400000)
            : startDay + 1;
          const barWidth = Math.max(1, endDay - startDay) * dayWidth;
          const barLeft = startDay * dayWidth;

          return (
            <div key={task.id} className="flex items-center h-8 border-b border-gray-100 hover:bg-gray-50">
              <div className="w-48 flex-shrink-0 text-xs truncate px-1 cursor-pointer hover:text-blue-600"
                onClick={() => onCardClick(task.id)} title={task.summary}>
                <span className="text-gray-400 mr-1">{task.documentNo}</span>
                {task.summary}
              </div>
              <div className="relative flex-1" style={{ height: 24 }}>
                <div
                  className={`absolute top-1 h-5 rounded text-[10px] text-white flex items-center px-1 truncate cursor-pointer hover:opacity-80 ${
                    task.isClosed ? 'bg-gray-400' : ''
                  }`}
                  style={{ left: barLeft, width: barWidth, minWidth: 20, backgroundColor: task.isClosed ? undefined : priorityHex(task.priority) }}
                  onClick={() => onCardClick(task.id)}
                  title={`${task.salesRepName} — ${task.statusName}`}
                >
                  {barWidth > 60 ? task.salesRepName : ''}
                </div>
              </div>
            </div>
          );
        })}

        {/* Today line */}
        <div className="absolute top-0 bottom-0 border-l-2 border-red-400 pointer-events-none z-20"
          style={{ left: 200 + todayOffset * dayWidth }} />
      </div>
    </div>
  );
}
