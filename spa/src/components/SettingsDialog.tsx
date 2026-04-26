import { useState } from 'react';
import { kanbanFetch } from '../api';
import { t } from '../i18n';
import { getDefaultColors, priorityLabel } from '../utils/priority';
import type { InitData, Status } from '../types';

interface Props {
  init: InitData;
  onClose: () => void;
  onSaved: (savedRequestTypeId?: number) => void;
  onError: (msg: string) => void;
}

export function SettingsDialog({ init, onClose, onSaved, onError }: Props) {
  const [tab, setTab] = useState<'source' | 'status' | 'colors'>('source');
  const [activeRtId, setActiveRtId] = useState(String(init.activeRequestTypeId || ''));
  const [wipLimits, setWipLimits] = useState<Record<string, string>>(() => {
    const m: Record<string, string> = {};
    for (const s of init.statuses) m[String(s.id)] = String(init.wipLimits?.[String(s.id)] || '');
    return m;
  });
  const [colors, setColors] = useState<Record<string, string>>(() => ({ ...getDefaultColors(), ...(init.priorityColors || {}) }));
  const [newStatusName, setNewStatusName] = useState('');
  const [newStatusType, setNewStatusType] = useState('open');
  const [saving, setSaving] = useState(false);

  const activeRt = init.requestTypes.find((rt) => rt.id === Number(activeRtId));
  const managedStatuses = activeRt
    ? init.statuses.filter((s) => s.statusCategoryId === activeRt.statusCategoryId)
    : [];

  async function save() {
    setSaving(true);
    try {
      const wipData: Record<string, number> = {};
      for (const [k, v] of Object.entries(wipLimits)) {
        const n = parseInt(v);
        if (!isNaN(n) && n >= 0) wipData[k] = n;
      }
      await kanbanFetch('/config', {
        method: 'POST',
        body: JSON.stringify({ activeRequestTypeId: Number(activeRtId), wipLimits: wipData, priorityColors: colors }),
      });
      onSaved(Number(activeRtId) || undefined);
      onClose();
    } catch (e: any) { onError(e.message); }
    setSaving(false);
  }

  async function addStatus() {
    if (!newStatusName.trim() || !activeRt) return;
    setSaving(true);
    try {
      await kanbanFetch('/config', {
        method: 'POST',
        body: JSON.stringify({
          createStatus: {
            name: newStatusName.trim(),
            statusCategoryId: activeRt.statusCategoryId,
            seqNo: (managedStatuses.length + 1) * 10,
            isOpen: newStatusType === 'open',
            isClosed: newStatusType === 'closed' || newStatusType === 'final',
            isFinalClose: newStatusType === 'final',
          },
        }),
      });
      setNewStatusName('');
      onSaved();
    } catch (e: any) { onError(e.message); }
    setSaving(false);
  }

  async function updateStatus(s: Status, updates: Partial<{ name: string; isOpen: boolean; isClosed: boolean; isFinalClose: boolean; seqNo: number }>) {
    try {
      await kanbanFetch('/config', {
        method: 'POST',
        body: JSON.stringify({
          updateStatus: { id: s.id, name: updates.name ?? s.name, seqNo: updates.seqNo ?? s.seqNo, isOpen: updates.isOpen ?? s.isOpen, isClosed: updates.isClosed ?? s.isClosed, isFinalClose: updates.isFinalClose ?? false },
        }),
      });
      onSaved();
    } catch (e: any) { onError(e.message); }
  }

  async function deleteStatus(sId: number) {
    try {
      await kanbanFetch('/config', { method: 'POST', body: JSON.stringify({ deleteStatusId: sId }) });
      onSaved();
    } catch (e: any) { onError(e.message); }
  }

  const tabs = [
    { key: 'source', label: t('KanbanBoardSource') },
    { key: 'status', label: t('KanbanStatusManagement') },
    { key: 'colors', label: t('KanbanPriorityColors') },
  ] as const;

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={onClose}>
      <div className="bg-white rounded-lg shadow-xl w-[600px] max-w-[90vw] max-h-[85vh] overflow-hidden flex flex-col" onClick={(e) => e.stopPropagation()}>
        <div className="text-sm font-semibold text-gray-700 p-4 pb-0">{t('KanbanSettings')}</div>
        <div className="flex border-b px-4 mt-2">
          {tabs.map((tb) => (
            <button key={tb.key} onClick={() => setTab(tb.key)}
              className={`text-xs px-3 py-2 border-b-2 ${tab === tb.key ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'}`}>
              {tb.label}
            </button>
          ))}
        </div>

        <div className="flex-1 overflow-y-auto p-4">
          {/* Board Source */}
          {tab === 'source' && (
            <div>
              <label className="text-xs text-gray-500">{t('KanbanRequestType')}</label>
              <select value={activeRtId} onChange={(e) => setActiveRtId(e.target.value)}
                className="w-full border rounded px-2 py-1.5 text-sm mt-1">
                {init.requestTypes.map((rt) => <option key={rt.id} value={rt.id}>{rt.name}</option>)}
              </select>
            </div>
          )}

          {/* Status Management + WIP (merged) */}
          {tab === 'status' && (
            <div>
              <div className="space-y-2 mb-3">
                {managedStatuses.map((s) => (
                  <div key={s.id} className="flex items-center gap-2 bg-gray-50 rounded p-2">
                    <span className="text-xs w-6 text-gray-400">{s.seqNo}</span>
                    <input defaultValue={s.name} className="flex-1 border rounded px-2 py-1 text-sm"
                      onBlur={(e) => { if (e.target.value !== s.name) updateStatus(s, { name: e.target.value }); }}
                      onKeyDown={(e) => { if (e.key === 'Enter' && !e.nativeEvent.isComposing) (e.target as HTMLInputElement).blur(); }} />
                    <select value={s.isFinalClose ? 'final' : s.isClosed ? 'closed' : 'open'}
                      onChange={(e) => {
                        const v = e.target.value;
                        updateStatus(s, { isOpen: v === 'open', isClosed: v === 'closed' || v === 'final', isFinalClose: v === 'final' });
                      }} className="text-xs border rounded px-1 py-1 w-24">
                      <option value="open">🟢 {t('KanbanStatusOpen')}</option>
                      <option value="closed">🔴 {t('KanbanStatusClosed')}</option>
                      <option value="final">⛔ {t('KanbanStatusFinalClose')}</option>
                    </select>
                    <input type="number" min="0" placeholder="WIP"
                      value={wipLimits[String(s.id)] || ''}
                      onChange={(e) => setWipLimits((prev) => ({ ...prev, [String(s.id)]: e.target.value }))}
                      className="w-16 border rounded px-1 py-1 text-xs text-center" title="WIP Limit (0=∞)" />
                    <button onClick={() => { if (confirm(t('KanbanDeleteStatus') + '?')) deleteStatus(s.id); }}
                      className="text-xs text-red-400 hover:text-red-600">✕</button>
                  </div>
                ))}
              </div>
              <div className="flex gap-2">
                <input value={newStatusName} onChange={(e) => setNewStatusName(e.target.value)}
                  placeholder={t('KanbanStatusName')} className="flex-1 border rounded px-2 py-1 text-sm"
                  onKeyDown={(e) => { if (e.key === 'Enter' && !e.nativeEvent.isComposing) addStatus(); }} />
                <select value={newStatusType} onChange={(e) => setNewStatusType(e.target.value)} className="text-xs border rounded px-1 py-1">
                  <option value="open">🟢</option>
                  <option value="closed">🔴</option>
                  <option value="final">⛔</option>
                </select>
                <button onClick={addStatus} disabled={!newStatusName.trim() || saving}
                  className="text-xs bg-blue-500 text-white px-3 py-1 rounded disabled:opacity-50">
                  {t('KanbanAddStatus')}
                </button>
              </div>
            </div>
          )}

          {/* Priority Colors */}
          {tab === 'colors' && (
            <div className="space-y-1">
              {['1', '3', '5', '7', '9'].map((p) => (
                <div key={p} className="flex items-center gap-2">
                  <span className="text-sm text-gray-700 w-24">{priorityLabel(p)}</span>
                  <input type="color" value={colors[p] || '#D1D5DB'}
                    onChange={(e) => setColors((prev) => ({ ...prev, [p]: e.target.value }))}
                    className="w-10 h-8 border rounded cursor-pointer" />
                  <span className="text-xs text-white px-2 py-0.5 rounded" style={{ backgroundColor: colors[p] }}>
                    {priorityLabel(p)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="flex justify-end gap-2 p-4 border-t">
          <button onClick={onClose} className="text-sm px-3 py-1.5 rounded bg-gray-100 text-gray-600">{t('KanbanCancel')}</button>
          <button onClick={save} disabled={saving} className="text-sm px-3 py-1.5 rounded bg-blue-500 text-white disabled:opacity-50">
            {saving ? t('KanbanSaving') : t('KanbanSave')}
          </button>
        </div>
      </div>
    </div>
  );
}
