import type { RequestType } from '../types';

const SCOPES = ['Private', 'Subordinates', 'All'] as const;

export function ScopeFilter({ scope, onScopeChange, requestTypes, requestTypeId, onRequestTypeChange }: {
  scope: string;
  onScopeChange: (s: string) => void;
  requestTypes: RequestType[];
  requestTypeId?: number;
  onRequestTypeChange: (id?: number) => void;
}) {
  return (
    <div className="flex items-center gap-3 px-4 py-2 bg-white border-b border-gray-200">
      <div className="flex gap-1">
        {SCOPES.map((s) => (
          <button
            key={s}
            onClick={() => onScopeChange(s)}
            className={`px-3 py-1 text-sm rounded ${
              scope === s ? 'bg-blue-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            {s}
          </button>
        ))}
      </div>
      <select
        value={requestTypeId ?? ''}
        onChange={(e) => onRequestTypeChange(e.target.value ? Number(e.target.value) : undefined)}
        className="text-sm border border-gray-300 rounded px-2 py-1"
      >
        <option value="">All Types</option>
        {requestTypes.map((rt) => (
          <option key={rt.id} value={rt.id}>{rt.name}</option>
        ))}
      </select>
    </div>
  );
}
