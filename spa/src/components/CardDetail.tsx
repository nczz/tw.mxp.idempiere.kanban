import { t } from "../i18n";
import { useState } from 'react';
import { useCardDetail, useUpdateCard, useAddComment } from '../hooks/useCards';
import { zoomRecord } from '../api';
import { priorityColor, priorityLabel } from '../utils/priority';
import { SearchSelect } from './SearchSelect';
import type { InitData } from '../types';

interface Props {
  cardId: number;
  init: InitData;
  onClose: () => void;
  onError: (msg: string) => void;
}

export function CardDetail({ cardId, init, onClose, onError }: Props) {
  const { data: card, isLoading } = useCardDetail(cardId);
  const updateCard = useUpdateCard();
  const addComment = useAddComment();
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState<Record<string, unknown>>({});
  const [commentText, setCommentText] = useState('');

  if (isLoading) return <Modal onClose={onClose}><div className="p-8 text-center text-gray-400">Loading...</div></Modal>;
  if (!card) return <Modal onClose={onClose}><div className="p-8 text-center text-red-500">Card not found</div></Modal>;

  function startEdit() {
    setForm({
      summary: card!.summary,
      result: card!.result || '',
      priority: card!.priority,
      statusId: card!.statusId,
      salesRepId: card!.salesRepId,
      requestTypeId: card!.requestTypeId,
      dateNextAction: card!.dateNextAction ? new Date(card!.dateNextAction).toISOString().slice(0, 16) : '',
      bpartnerId: card!.bpartnerId || undefined,
      bpartnerName: card!.bpartnerName || '',
      productId: card!.productId || undefined,
      productName: card!.productName || '',
      orderId: card!.orderId || undefined,
      orderName: card!.orderName || '',
      invoiceId: card!.invoiceId || undefined,
      invoiceName: card!.invoiceName || '',
      paymentId: card!.paymentId || undefined,
      paymentName: card!.paymentName || '',
      projectId: card!.projectId || undefined,
      projectName: card!.projectName || '',
      campaignId: card!.campaignId || undefined,
      campaignName: card!.campaignName || '',
      assetId: card!.assetId || undefined,
      assetName: card!.assetName || '',
      activityId: card!.activityId || undefined,
      activityName: card!.activityName || '',
    });
    setEditing(true);
  }

  function saveEdit() {
    const data: Record<string, unknown> = { id: cardId };
    if (form.summary !== card!.summary) data.summary = form.summary;
    if (form.result !== (card!.result || '')) data.result = form.result;
    if (form.priority !== card!.priority) data.priority = form.priority;
    if (Number(form.statusId) !== card!.statusId) data.statusId = Number(form.statusId);
    if (Number(form.salesRepId) !== card!.salesRepId) data.salesRepId = Number(form.salesRepId);
    if (Number(form.requestTypeId) !== card!.requestTypeId) data.requestTypeId = Number(form.requestTypeId);
    if (form.dateNextAction) data.dateNextAction = new Date(form.dateNextAction as string).getTime();
    // ERP links
    const fkFields = ['bpartnerId','productId','orderId','invoiceId','paymentId','projectId','campaignId','assetId','activityId'];
    for (const fk of fkFields) {
      const newVal = (form[fk] as number | undefined) || 0;
      const oldVal = ((card as unknown as Record<string, unknown>)[fk] as number | undefined) || 0;
      if (newVal !== oldVal) data[fk] = newVal;
    }

    updateCard.mutate(data as { id: number }, {
      onSuccess: () => setEditing(false),
      onError: (e) => onError(e.message),
    });
  }

  const set = (key: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const fmtDate = (ts: number | null) => ts ? new Date(ts).toLocaleString() : '—';

  // Filter statuses by the request type being edited (not the original)
  const editingRtId = editing ? Number(form.requestTypeId) : card.requestTypeId;
  const rt = init.requestTypes.find((r) => r.id === editingRtId);
  const availableStatuses = rt
    ? init.statuses.filter((s) => s.statusCategoryId === rt.statusCategoryId)
    : init.statuses;

  return (
    <Modal onClose={onClose}>
      <div className="max-h-[80vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <span className="text-xs text-gray-400 font-mono">{card.documentNo}</span>
            {card.priority && (
              <span className={`text-xs text-white px-1.5 py-0.5 rounded ${priorityColor(card.priority)}`}>
                {priorityLabel(card.priority)}
              </span>
            )}
            {card.isEscalated && <span className="text-xs bg-red-100 text-red-600 px-1.5 py-0.5 rounded">{t("KanbanEscalated")}</span>}
          </div>
          <div className="flex gap-2">
            {!editing && <button onClick={startEdit} className="text-xs bg-blue-500 text-white px-3 py-1 rounded hover:bg-blue-600">{t("KanbanEdit")}</button>}
            {editing && <button onClick={saveEdit} disabled={updateCard.isPending} className="text-xs bg-green-500 text-white px-3 py-1 rounded hover:bg-green-600 disabled:opacity-50">{updateCard.isPending ? t('KanbanSaving') : t('KanbanSave')}</button>}
            {editing && <button onClick={() => setEditing(false)} className="text-xs bg-gray-300 text-gray-700 px-3 py-1 rounded">{t("KanbanCancel")}</button>}
          </div>
        </div>

        {editing ? (
          /* ===== EDIT MODE ===== */
          <div className="space-y-3 mb-4">
            <div>
              <label className="text-xs text-gray-500">{t("KanbanSummary")}</label>
              <input value={form.summary as string} onChange={set('summary')} className="w-full border rounded px-2 py-1 text-sm mt-0.5" />
            </div>
            <div>
              <label className="text-xs text-gray-500">{t("KanbanNotesResult")}</label>
              <textarea value={form.result as string} onChange={set('result')} className="w-full border rounded px-2 py-1 text-sm mt-0.5 h-20" />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-xs text-gray-500">{t("KanbanPriority")}</label>
                <select value={form.priority as string} onChange={set('priority')} className="w-full border rounded px-2 py-1 text-sm mt-0.5">
                  {init.priorities.map((p) => <option key={p.value} value={p.value}>{p.name}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs text-gray-500">{t("KanbanStatus")}</label>
                <select value={form.statusId as number} onChange={set('statusId')} className="w-full border rounded px-2 py-1 text-sm mt-0.5">
                  {availableStatuses.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs text-gray-500">{t("KanbanSalesRep")}</label>
                <select value={form.salesRepId as number} onChange={set('salesRepId')} className="w-full border rounded px-2 py-1 text-sm mt-0.5">
                  {init.salesReps.map((sr) => <option key={sr.id} value={sr.id}>{sr.name}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs text-gray-500">{t("KanbanRequestType")}</label>
                <select value={form.requestTypeId as number} onChange={(e) => {
                  const newRtId = Number(e.target.value);
                  setForm((f) => {
                    // When type changes, check if current status is still valid
                    const newRt = init.requestTypes.find((r) => r.id === newRtId);
                    const newStatuses = newRt ? init.statuses.filter((s) => s.statusCategoryId === newRt.statusCategoryId) : init.statuses;
                    const currentStatusValid = newStatuses.some((s) => s.id === Number(f.statusId));
                    return {
                      ...f,
                      requestTypeId: newRtId,
                      statusId: currentStatusValid ? f.statusId : (newStatuses.find((s) => s.isOpen)?.id || newStatuses[0]?.id || f.statusId),
                    };
                  });
                }} className="w-full border rounded px-2 py-1 text-sm mt-0.5">
                  {init.requestTypes.map((rt) => <option key={rt.id} value={rt.id}>{rt.name}</option>)}
                </select>
              </div>
              <div className="col-span-2">
                <label className="text-xs text-gray-500">{t("KanbanDateNextAction")}</label>
                <input type="datetime-local" value={form.dateNextAction as string} onChange={set('dateNextAction')} className="w-full border rounded px-2 py-1 text-sm mt-0.5" />
              </div>
            </div>
            {/* ERP Links (edit) */}
            <div className="text-xs font-semibold text-gray-500 mt-3 mb-1">ERP Links</div>
            <div className="grid grid-cols-2 gap-3">
              <SearchSelect table="C_BPartner" label={t("KanbanBusinessPartner")}
                value={form.bpartnerId as number | undefined} valueName={form.bpartnerName as string}
                onChange={(id, name) => setForm((f) => ({ ...f, bpartnerId: id, bpartnerName: name }))} />
              <SearchSelect table="M_Product" label={t("KanbanProduct")}
                value={form.productId as number | undefined} valueName={form.productName as string}
                onChange={(id, name) => setForm((f) => ({ ...f, productId: id, productName: name }))} />
              <SearchSelect table="C_Order" label={t("KanbanOrder")}
                value={form.orderId as number | undefined} valueName={form.orderName as string}
                onChange={(id, name) => setForm((f) => ({ ...f, orderId: id, orderName: name }))} />
              <SearchSelect table="C_Invoice" label={t("KanbanInvoice")}
                value={form.invoiceId as number | undefined} valueName={form.invoiceName as string}
                onChange={(id, name) => setForm((f) => ({ ...f, invoiceId: id, invoiceName: name }))} />
              <SearchSelect table="C_Payment" label={t("KanbanPayment")}
                value={form.paymentId as number | undefined} valueName={form.paymentName as string}
                onChange={(id, name) => setForm((f) => ({ ...f, paymentId: id, paymentName: name }))} />
              <SearchSelect table="C_Project" label={t("KanbanProject")}
                value={form.projectId as number | undefined} valueName={form.projectName as string}
                onChange={(id, name) => setForm((f) => ({ ...f, projectId: id, projectName: name }))} />
              <SearchSelect table="C_Campaign" label={t("KanbanCampaign")}
                value={form.campaignId as number | undefined} valueName={form.campaignName as string}
                onChange={(id, name) => setForm((f) => ({ ...f, campaignId: id, campaignName: name }))} />
              <SearchSelect table="A_Asset" label={t("KanbanAsset")}
                value={form.assetId as number | undefined} valueName={form.assetName as string}
                onChange={(id, name) => setForm((f) => ({ ...f, assetId: id, assetName: name }))} />
              <SearchSelect table="C_Activity" label={t("KanbanActivity")}
                value={form.activityId as number | undefined} valueName={form.activityName as string}
                onChange={(id, name) => setForm((f) => ({ ...f, activityId: id, activityName: name }))} />
            </div>
          </div>
        ) : (
          /* ===== VIEW MODE ===== */
          <>
            <div className="text-base font-medium text-gray-800 mb-1">{card.summary}</div>
            <div className="text-xs text-gray-400 mb-1">Notes / Result</div>
            <div className="text-sm text-gray-600 bg-gray-50 rounded p-2 min-h-[2rem] whitespace-pre-wrap mb-3">
              {card.result || <span className="text-gray-300 italic">No notes</span>}
            </div>
            <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-xs mb-3">
              <Field label={t("KanbanStatus")} value={card.statusName} />
              <Field label={t("KanbanRequestType")} value={card.requestTypeName} />
              <Field label={t("KanbanPriority")} value={priorityLabel(card.priority)} />
              <Field label={t("KanbanSalesRep")} value={card.salesRepName} />
              <Field label={t("KanbanRequester")} value={card.requesterName} />
              <Field label={t("KanbanCreatedBy")} value={card.creatorName} />
              <Field label={t("KanbanCreated")} value={fmtDate(card.created)} />
              <Field label={t("KanbanNextAction")} value={fmtDate(card.dateNextAction)} />
              <Field label={t("KanbanStartDate")} value={fmtDate(card.startDate)} />
              {card.closeDate && <Field label={t("KanbanCloseDate")} value={fmtDate(card.closeDate)} />}
            </div>
          </>
        )}

        {/* ERP Links */}
        <div className="mb-3">
          <div className="text-xs font-semibold text-gray-500 mb-1">ERP Links</div>
          <div className="flex flex-wrap gap-1">
            {card.bpartnerId && <ZoomChip label={`🏢 ${card.bpartnerName}`} table="C_BPartner" id={card.bpartnerId} />}
            {card.productId && <ZoomChip label={`📦 ${card.productName || 'Product'}`} table="M_Product" id={card.productId} />}
            {card.orderId && <ZoomChip label={`📋 ${card.orderName || 'Order'}`} table="C_Order" id={card.orderId} />}
            {card.invoiceId && <ZoomChip label={`🧾 ${card.invoiceName || 'Invoice'}`} table="C_Invoice" id={card.invoiceId} />}
            {card.paymentId && <ZoomChip label={`💳 ${card.paymentName || 'Payment'}`} table="C_Payment" id={card.paymentId} />}
            {card.projectId && <ZoomChip label={`📁 ${card.projectName || 'Project'}`} table="C_Project" id={card.projectId} />}
            {card.campaignId && <ZoomChip label={`📣 ${card.campaignName || 'Campaign'}`} table="C_Campaign" id={card.campaignId} />}
            {card.assetId && <ZoomChip label={`🔧 ${card.assetName || 'Asset'}`} table="A_Asset" id={card.assetId} />}
            {card.activityId && <ZoomChip label={`📊 ${card.activityName || 'Activity'}`} table="C_Activity" id={card.activityId} />}
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
                  <span className="bg-gray-100 px-1 rounded">{h.fromStatus || '—'}</span>
                  <span>→</span>
                  <span className="bg-blue-100 px-1 rounded">{h.toStatus}</span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Comments */}
        <div className="mt-3">
          <div className="text-xs font-semibold text-gray-500 mb-1">{t("KanbanComments")}</div>
          <div className="flex gap-2 mb-2">
            <input value={commentText} onChange={(e) => setCommentText(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter' && !e.nativeEvent.isComposing && commentText.trim()) { addComment.mutate({ cardId, text: commentText.trim() }, { onSuccess: () => setCommentText('') }); } }}
              placeholder={t("KanbanAddComment")} className="flex-1 border rounded px-2 py-1 text-sm" />
            <button onClick={() => { if (commentText.trim()) addComment.mutate({ cardId, text: commentText.trim() }, { onSuccess: () => setCommentText('') }); }}
              disabled={!commentText.trim() || addComment.isPending}
              className="text-xs bg-blue-500 text-white px-3 py-1 rounded hover:bg-blue-600 disabled:opacity-50">
              {addComment.isPending ? t("KanbanPosting") : t("KanbanPost")}
            </button>
          </div>
          {card.comments && card.comments.length > 0 ? (
            <div className="space-y-2 max-h-48 overflow-y-auto">
              {card.comments.map((c) => (
                <div key={c.id} className="bg-gray-50 rounded p-2">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs font-medium text-gray-700">👤 {c.userName}</span>
                    <span className="text-xs text-gray-400">{new Date(c.date).toLocaleString()}</span>
                  </div>
                  <div className="text-sm text-gray-600 whitespace-pre-wrap">{c.text}</div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-xs text-gray-400">{t("KanbanNoComments")}</div>
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
    <button onClick={() => zoomRecord(table, id)}
      className="text-xs bg-blue-50 text-blue-600 px-2 py-0.5 rounded hover:bg-blue-100 transition-colors">
      {label}
    </button>
  );
}
