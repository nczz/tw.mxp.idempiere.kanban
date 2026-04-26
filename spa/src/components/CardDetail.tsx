import { useState } from 'react';
import { useCardDetail, useUpdateCard } from '../hooks/useCards';
import { zoomRecord } from '../api';
import { priorityColor, priorityLabel } from '../utils/priority';
import type { Status } from '../types';

interface Props {
  cardId: number;
  statuses: Status[];
  onClose: () => void;
  onError: (msg: string) => void;
}

export function CardDetail({ cardId, statuses, onClose, onError }: Props) {
  const { data: card, isLoading } = useCardDetail(cardId);
  const updateCard = useUpdateCard();
  const [editing, setEditing] = useState(false);
  const [summary, setSummary] = useState('');
  const [result, setResult] = useState('');

  if (isLoading) return <Modal onClose={onClose}><div className="p-8 text-center text-gray-400">Loading...</div></Modal>;
  if (!card) return <Modal onClose={onClose}><div className="p-8 text-center text-red-500">Card not found</div></Modal>;

  function startEdit() {
    setSummary(card!.summary);
    setResult(card!.result || '');
    setEditing(true);
  }

  function saveEdit() {
    updateCard.mutate(
      { id: cardId, summary, result },
      {
        onSuccess: () => setEditing(false),
        onError: (e) => onError(e.message),
      }
    );
  }

  const fmtDate = (ts: number | null) => ts ? new Date(ts).toLocaleString() : '—';

  return (
    <Modal onClose={onClose}>
      <div className="max-h-[80vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <span className="text-xs text-gray-400 font-mono">{card.documentNo}</span>
            {card.priority && (
              <span className={`text-xs text-white px-1.5 py-0.5 rounded ${priorityColor(card.priority)}`}>
                {priorityLabel(card.priority)}
              </span>
            )}
            {card.isEscalated && <span className="text-xs bg-red-100 text-red-600 px-1.5 py-0.5 rounded">Escalated</span>}
          </div>
          <div className="flex gap-2">
            {!editing && <button onClick={startEdit} className="text-xs bg-blue-500 text-white px-3 py-1 rounded hover:bg-blue-600">Edit</button>}
            {editing && <button onClick={saveEdit} className="text-xs bg-green-500 text-white px-3 py-1 rounded hover:bg-green-600" disabled={updateCard.isPending}>Save</button>}
            {editing && <button onClick={() => setEditing(false)} className="text-xs bg-gray-300 text-gray-700 px-3 py-1 rounded">Cancel</button>}
          </div>
        </div>

        {/* Summary + Result */}
        {editing ? (
          <div className="space-y-2 mb-4">
            <input value={summary} onChange={(e) => setSummary(e.target.value)}
              className="w-full border rounded px-2 py-1 text-sm" placeholder="Summary" />
            <textarea value={result} onChange={(e) => setResult(e.target.value)}
              className="w-full border rounded px-2 py-1 text-sm h-20" placeholder="Notes / Result" />
          </div>
        ) : (
          <div className="mb-4">
            <div className="text-base font-medium text-gray-800 mb-1">{card.summary}</div>
            {card.result && <div className="text-sm text-gray-600 bg-gray-50 rounded p-2">{card.result}</div>}
          </div>
        )}

        {/* Info grid */}
        <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-xs mb-4">
          <Field label="Status" value={card.statusName} />
          <Field label="Request Type" value={card.requestTypeName} />
          <Field label="Sales Rep" value={card.salesRepName} />
          <Field label="Requester" value={card.requesterName} />
          <Field label="Created By" value={card.creatorName} />
          <Field label="Created" value={fmtDate(card.created)} />
          <Field label="Next Action" value={fmtDate(card.dateNextAction)} />
          <Field label="Start Date" value={fmtDate(card.startDate)} />
        </div>

        {/* ERP Links */}
        <div className="mb-4">
          <div className="text-xs font-semibold text-gray-500 mb-1">ERP Links</div>
          <div className="flex flex-wrap gap-1">
            {card.bpartnerId && <ZoomChip label={`🏢 ${card.bpartnerName}`} table="C_BPartner" id={card.bpartnerId} />}
            {card.productId && <ZoomChip label="📦 Product" table="M_Product" id={card.productId} />}
            {card.orderId && <ZoomChip label="📋 Order" table="C_Order" id={card.orderId} />}
            {card.invoiceId && <ZoomChip label="🧾 Invoice" table="C_Invoice" id={card.invoiceId} />}
            {card.paymentId && <ZoomChip label="💳 Payment" table="C_Payment" id={card.paymentId} />}
            {card.projectId && <ZoomChip label="📁 Project" table="C_Project" id={card.projectId} />}
            {card.campaignId && <ZoomChip label="📣 Campaign" table="C_Campaign" id={card.campaignId} />}
            {card.assetId && <ZoomChip label="🔧 Asset" table="A_Asset" id={card.assetId} />}
            {!card.bpartnerId && !card.productId && !card.orderId && !card.invoiceId &&
             !card.projectId && <span className="text-xs text-gray-400">No linked records</span>}
          </div>
        </div>

        {/* Move History */}
        <div>
          <div className="text-xs font-semibold text-gray-500 mb-1">Move History</div>
          {card.moveHistory.length === 0 ? (
            <div className="text-xs text-gray-400">No moves recorded</div>
          ) : (
            <div className="space-y-1 max-h-40 overflow-y-auto">
              {card.moveHistory.map((h, i) => (
                <div key={i} className="text-xs flex items-center gap-2 text-gray-600">
                  <span className="text-gray-400 w-32 flex-shrink-0">{new Date(h.date).toLocaleString()}</span>
                  <span className="font-medium">{h.userName}</span>
                  <span>moved</span>
                  <span className="bg-gray-100 px-1 rounded">{h.fromStatus || '—'}</span>
                  <span>→</span>
                  <span className="bg-blue-100 px-1 rounded">{h.toStatus}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </Modal>
  );
}

function Modal({ children, onClose }: { children: React.ReactNode; onClose: () => void }) {
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={onClose}>
      <div className="bg-white rounded-lg shadow-xl w-[600px] max-w-[90vw] p-5" onClick={(e) => e.stopPropagation()}>
        {children}
      </div>
    </div>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between py-0.5 border-b border-gray-100">
      <span className="text-gray-400">{label}</span>
      <span className="text-gray-700 text-right">{value}</span>
    </div>
  );
}

function ZoomChip({ label, table, id }: { label: string; table: string; id: number }) {
  return (
    <button
      onClick={() => zoomRecord(table, id)}
      className="text-xs bg-blue-50 text-blue-600 px-2 py-0.5 rounded hover:bg-blue-100 transition-colors"
    >
      {label}
    </button>
  );
}
