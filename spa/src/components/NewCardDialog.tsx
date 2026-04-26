import { t } from "../i18n";
import { useState } from 'react';
import { useCreateCard } from '../hooks/useCards';
import { SearchSelect } from './SearchSelect';
import type { InitData } from '../types';

interface Props {
  init: InitData;
  requestTypeId?: number;
  orgId?: number;
  onClose: () => void;
  onError: (msg: string) => void;
}

export function NewCardDialog({ init, requestTypeId: activeRtId, orgId: filterOrgId, onClose, onError }: Props) {
  const [summary, setSummary] = useState('');
  const [orgId, setOrgId] = useState<string>(String(filterOrgId || init.orgs?.[0]?.id || ''));
  const [result, setResult] = useState('');
  const [requestTypeId, setRequestTypeId] = useState<string>(String(activeRtId || init.requestTypes[0]?.id || ''));
  const activeRt = init.requestTypes.find((rt) => rt.id === Number(requestTypeId));
  const availableStatuses = activeRt
    ? init.statuses.filter((s) => s.statusCategoryId === activeRt.statusCategoryId && !s.isClosed)
    : init.statuses.filter((s) => !s.isClosed);
  const [statusId, setStatusId] = useState<string>(availableStatuses[0]?.id?.toString() || '');
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
        statusId: statusId ? Number(statusId) : undefined,
        orgId: orgId ? Number(orgId) : undefined,
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
        <div className="text-sm font-semibold text-gray-700 mb-3">{t("KanbanNewRequest")}</div>
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="text-xs text-gray-500">{t("KanbanSummary")} *</label>
            <input value={summary} onChange={(e) => setSummary(e.target.value)}
              className="w-full border rounded px-2 py-1.5 text-sm mt-0.5" autoFocus placeholder={t("KanbanDescribeRequest")} />
          </div>
          <div>
            <label className="text-xs text-gray-500">{t("KanbanNotesResult")}</label>
            <textarea value={result} onChange={(e) => setResult(e.target.value)}
              className="w-full border rounded px-2 py-1.5 text-sm mt-0.5 h-16" placeholder={t("KanbanAdditionalDetails")} />
          </div>
          {init.orgs && init.orgs.length > 1 && (
            <div>
              <label className="text-xs text-gray-500">Org</label>
              <select value={orgId} onChange={(e) => setOrgId(e.target.value)}
                className="w-full border rounded px-2 py-1.5 text-sm mt-0.5">
                {init.orgs.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
              </select>
            </div>
          )}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-gray-500">{t("KanbanRequestType")}</label>
              <select value={requestTypeId} onChange={(e) => { setRequestTypeId(e.target.value); setStatusId(''); }}
                className="w-full border rounded px-2 py-1.5 text-sm mt-0.5">
                <option value="">{t("KanbanSelectNone")}</option>
                {init.requestTypes.map((rt) => <option key={rt.id} value={rt.id}>{rt.name}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500">{t("KanbanStatus")}</label>
              <select value={statusId} onChange={(e) => setStatusId(e.target.value)}
                className="w-full border rounded px-2 py-1.5 text-sm mt-0.5">
                {availableStatuses.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500">{t("KanbanPriority")}</label>
              <select value={priority} onChange={(e) => setPriority(e.target.value)}
                className="w-full border rounded px-2 py-1.5 text-sm mt-0.5">
                {init.priorities.map((p) => <option key={p.value} value={p.value}>{p.name}</option>)}
              </select>
            </div>
            <div>
              <SearchSelect table="AD_User" label={t("KanbanSalesRep")}
                value={salesRepId ? Number(salesRepId) : undefined}
                valueName={init.user.name}
                onChange={(id) => setSalesRepId(id ? String(id) : '')} />
            </div>
            <div>
              <label className="text-xs text-gray-500">{t("KanbanDateNextAction")}</label>
              <input type="datetime-local" value={dateNextAction} onChange={(e) => setDateNextAction(e.target.value)}
                className="w-full border rounded px-2 py-1.5 text-sm mt-0.5" />
            </div>
          </div>

          {/* ERP Links */}
          <div className="text-xs font-semibold text-gray-500 mt-1">ERP Links</div>
          <div className="grid grid-cols-2 gap-3">
            <SearchSelect table="C_BPartner" label={t("KanbanBusinessPartner")} value={erp.bpartnerId} onChange={setFk('bpartnerId')} />
            <SearchSelect table="M_Product" label={t("KanbanProduct")} value={erp.productId} onChange={setFk('productId')} />
            <SearchSelect table="C_Project" label={t("KanbanProject")} value={erp.projectId} onChange={setFk('projectId')} />
            <SearchSelect table="C_Campaign" label={t("KanbanCampaign")} value={erp.campaignId} onChange={setFk('campaignId')} />
            <SearchSelect table="C_Activity" label={t("KanbanActivity")} value={erp.activityId} onChange={setFk('activityId')} />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose} className="text-sm px-3 py-1.5 rounded bg-gray-100 text-gray-600 hover:bg-gray-200">{t("KanbanCancel")}</button>
            <button type="submit" disabled={!summary.trim() || createCard.isPending}
              className="text-sm px-3 py-1.5 rounded bg-blue-500 text-white hover:bg-blue-600 disabled:opacity-50">
              {createCard.isPending ? t('KanbanCreating') : t('KanbanCreate')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
