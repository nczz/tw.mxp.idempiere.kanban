import { useState } from 'react';
import { useCreateCard } from '../hooks/useCards';
import type { RequestType } from '../types';

interface Props {
  requestTypes: RequestType[];
  onClose: () => void;
  onError: (msg: string) => void;
}

export function NewCardDialog({ requestTypes, onClose, onError }: Props) {
  const [summary, setSummary] = useState('');
  const [requestTypeId, setRequestTypeId] = useState<number | undefined>();
  const [priority, setPriority] = useState('5');
  const createCard = useCreateCard();

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!summary.trim()) return;
    createCard.mutate(
      { summary: summary.trim(), requestTypeId, priority: parseInt(priority) },
      {
        onSuccess: () => onClose(),
        onError: (err) => onError(err.message),
      }
    );
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={onClose}>
      <div className="bg-white rounded-lg shadow-xl w-[450px] max-w-[90vw] p-5" onClick={(e) => e.stopPropagation()}>
        <div className="text-sm font-semibold text-gray-700 mb-3">New Request</div>
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="text-xs text-gray-500">Summary *</label>
            <input value={summary} onChange={(e) => setSummary(e.target.value)}
              className="w-full border rounded px-2 py-1.5 text-sm mt-0.5" autoFocus placeholder="Describe the request..." />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-gray-500">Request Type</label>
              <select value={requestTypeId ?? ''} onChange={(e) => setRequestTypeId(e.target.value ? Number(e.target.value) : undefined)}
                className="w-full border rounded px-2 py-1.5 text-sm mt-0.5">
                <option value="">— Select —</option>
                {requestTypes.map((rt) => <option key={rt.id} value={rt.id}>{rt.name}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500">Priority</label>
              <select value={priority} onChange={(e) => setPriority(e.target.value)}
                className="w-full border rounded px-2 py-1.5 text-sm mt-0.5">
                <option value="1">Urgent</option>
                <option value="3">High</option>
                <option value="5">Medium</option>
                <option value="7">Low</option>
                <option value="9">Minor</option>
              </select>
            </div>
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose} className="text-sm px-3 py-1.5 rounded bg-gray-100 text-gray-600 hover:bg-gray-200">Cancel</button>
            <button type="submit" disabled={!summary.trim() || createCard.isPending}
              className="text-sm px-3 py-1.5 rounded bg-blue-500 text-white hover:bg-blue-600 disabled:opacity-50">
              {createCard.isPending ? 'Creating...' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
