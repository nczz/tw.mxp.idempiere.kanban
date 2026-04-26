import { t } from '../i18n';
import type { RequestType } from '../types';

const SCOPES = [
  { key: 'Private', msgKey: 'KanbanPrivate' },
  { key: 'Subordinates', msgKey: 'KanbanSubordinates' },
  { key: 'All', msgKey: 'KanbanAll' },
] as const;

export function ScopeFilter({ scope, onScopeChange, requestTypes, requestTypeId, onRequestTypeChange }: {
  scope: string;
  onScopeChange: (s: string) => void;
  requestTypes: RequestType[];
  requestTypeId?: number;
  onRequestTypeChange: (id?: number) => void;
}) {
  return (
    <>
      <div className="flex gap-1">
        {SCOPES.map((s) => (
          <button key={s.key} onClick={() => onScopeChange(s.key)}
            className={`px-3 py-1 text-sm rounded ${scope === s.key ? 'bg-blue-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}>
            {t(s.msgKey)}
          </button>
        ))}
      </div>
      <select value={requestTypeId ?? ''} onChange={(e) => onRequestTypeChange(e.target.value ? Number(e.target.value) : undefined)}
        className="text-sm border border-gray-300 rounded px-2 py-1">
        <option value="">{t('KanbanAllTypes')}</option>
        {requestTypes.map((rt) => <option key={rt.id} value={rt.id}>{rt.name}</option>)}
      </select>
    </>
  );
}
