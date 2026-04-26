import { useState } from 'react';
import { useCreateCard } from '../hooks/useCards';
import { SearchSelect } from './SearchSelect';
import type { InitData } from '../types';

interface Props {
  init: InitData;
  onClose: () => void;
  onError: (msg: string) => void;
}

export function NewCardDialog({ init, onClose, onError }: Props) {
  const [summary, setSummary] = useState('');
  const [result, setResult] = useState('');
  const [requestTypeId, setRequestTypeId] = useState<string>(init.requestTypes[0]?.id?.toString() || '');
  const [priority, setPriority] = useState('5');
  const [salesRepId, setSalesRepId] = useState<string>(init.user.id.toString());
  const [dateNextAction, setDateNextAction] = useState('');
  const [erp, setErp] = useState<Record<string, number | undefined>>({});
  const createCard = useCreateCard();

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!summary.trim()) return;
    createCard.mutate(
      {
        summary: summary.trim(),
        result: result.trim() || undefined,
        requestTypeId: requestTypeId ? Number(requestTypeId) : undefined,
        priority: Number(priority),
        salesRepId: salesRepId ? Number(salesRepId) : undefined,
        dateNextAction: dateNextAction ? new Date(dateNextAction).getTime() : undefined,
        bpartnerId: erp.bpartnerId,
        productId: erp.productId,
        projectId: erp.projectId,
        campaignId: erp.campaignId,
        activityId: erp.activityId,
      },
      {
        onSuccess: () => onClose(),
        onError: (err) => onError(err.message),
      }
    );
  }

  const setFk = (key: string) => (id: number | undefined) =>
    setErp((prev) => ({ ...prev, [key]: id }));

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={onClose}>
      <div className="bg-white rounded-lg shadow-xl w-[520px] max-w-[90vw] max-h-[85vh] overflow-y-auto p-5" onClick={(e) => e.stopPropagation()}>
        <div className="text-sm font-semibold text-gray-700 mb-3">New Request</div>
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="text-xs text-gray-500">Summary *</label>
            <input value={summary} onChange={(e) => setSummary(e.target.value)}
              className="w-full border rounded px-2 py-1.5 text-sm mt-0.5" autoFocus placeholder="Describe the request..." />
          </div>
          <div>
            <label className="text-xs text-gray-500">Notes / Result</label>
            <textarea value={result} onChange={(e) => setResult(e.target.value)}
              className="w-full border rounded px-2 py-1.5 text-sm mt-0.5 h-16" placeholder="Additional details..." />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-gray-500">Request Type</label>
              <select value={requestTypeId} onChange={(e) => setRequestTypeId(e.target.value)}
                className="w-full border rounded px-2 py-1.5 text-sm mt-0.5">
                <option value="">— Select —</option>
                {init.requestTypes.map((rt) => <option key={rt.id} value={rt.id}>{rt.name}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500">Priority</label>
              <select value={priority} onChange={(e) => setPriority(e.target.value)}
                className="w-full border rounded px-2 py-1.5 text-sm mt-0.5">
                {init.priorities.map((p) => <option key={p.value} value={p.value}>{p.name}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500">Sales Rep</label>
              <select value={salesRepId} onChange={(e) => setSalesRepId(e.target.value)}
                className="w-full border rounded px-2 py-1.5 text-sm mt-0.5">
                {init.salesReps.map((sr) => <option key={sr.id} value={sr.id}>{sr.name}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500">Date Next Action</label>
              <input type="datetime-local" value={dateNextAction} onChange={(e) => setDateNextAction(e.target.value)}
                className="w-full border rounded px-2 py-1.5 text-sm mt-0.5" />
            </div>
          </div>

          {/* ERP Links */}
          <div className="text-xs font-semibold text-gray-500 mt-1">ERP Links</div>
          <div className="grid grid-cols-2 gap-3">
            <SearchSelect table="C_BPartner" label="Business Partner" value={erp.bpartnerId} onChange={setFk('bpartnerId')} />
            <SearchSelect table="M_Product" label="Product" value={erp.productId} onChange={setFk('productId')} />
            <SearchSelect table="C_Project" label="Project" value={erp.projectId} onChange={setFk('projectId')} />
            <SearchSelect table="C_Campaign" label="Campaign" value={erp.campaignId} onChange={setFk('campaignId')} />
            <SearchSelect table="C_Activity" label="Activity" value={erp.activityId} onChange={setFk('activityId')} />
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
