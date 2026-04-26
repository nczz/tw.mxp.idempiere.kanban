import { useState } from 'react';
import { kanbanFetch } from '../api';
import { t } from '../i18n';
import { getDefaultColors, priorityLabel } from '../utils/priority';
import type { InitData, Status } from '../types';

interface Props {
  init: InitData;
  statuses: Status[];
  onClose: () => void;
  onSaved: () => void;
  onError: (msg: string) => void;
}

export function SettingsDialog({ init, statuses, onClose, onSaved, onError }: Props) {
  const [wipLimits, setWipLimits] = useState<Record<string, string>>(() => {
    const m: Record<string, string> = {};
    for (const s of statuses) m[String(s.id)] = String(init.wipLimits?.[String(s.id)] || '');
    return m;
  });

  const [colors, setColors] = useState<Record<string, string>>(() => {
    const defaults = getDefaultColors();
    return { ...defaults, ...(init.priorityColors || {}) };
  });

  const [saving, setSaving] = useState(false);

  async function handleSave() {
    setSaving(true);
    try {
      const wipData: Record<string, number> = {};
      for (const [k, v] of Object.entries(wipLimits)) {
        const n = parseInt(v);
        if (!isNaN(n) && n >= 0) wipData[k] = n;
      }
      await kanbanFetch('/config', {
        method: 'POST',
        body: JSON.stringify({ wipLimits: wipData, priorityColors: colors }),
      });
      onSaved();
      onClose();
    } catch (e: any) {
      onError(e.message || 'Save failed');
    }
    setSaving(false);
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={onClose}>
      <div className="bg-white rounded-lg shadow-xl w-[500px] max-w-[90vw] max-h-[80vh] overflow-y-auto p-5" onClick={(e) => e.stopPropagation()}>
        <div className="text-sm font-semibold text-gray-700 mb-4">{t('KanbanSettings')}</div>

        {/* WIP Limits */}
        <div className="mb-4">
          <div className="text-xs font-semibold text-gray-500 mb-2">{t('KanbanWipLimits')}</div>
          <div className="space-y-1">
            {statuses.map((s) => (
              <div key={s.id} className="flex items-center gap-2">
                <span className="text-sm text-gray-700 w-40 truncate">{s.name}</span>
                <input type="number" min="0" placeholder="0 = ∞"
                  value={wipLimits[String(s.id)] || ''}
                  onChange={(e) => setWipLimits((prev) => ({ ...prev, [String(s.id)]: e.target.value }))}
                  className="w-20 border rounded px-2 py-1 text-sm text-center" />
                <span className="text-xs text-gray-400">{t('KanbanWipCards')}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Priority Colors */}
        <div className="mb-4">
          <div className="text-xs font-semibold text-gray-500 mb-2">{t('KanbanPriorityColors')}</div>
          <div className="space-y-1">
            {['1', '3', '5', '7', '9'].map((p) => (
              <div key={p} className="flex items-center gap-2">
                <span className="text-sm text-gray-700 w-24">{priorityLabel(p)}</span>
                <input type="color" value={colors[p] || '#D1D5DB'}
                  onChange={(e) => setColors((prev) => ({ ...prev, [p]: e.target.value }))}
                  className="w-10 h-8 border rounded cursor-pointer" />
                <span className="text-xs text-gray-400 font-mono">{colors[p]}</span>
                <span className="text-xs text-white px-2 py-0.5 rounded" style={{ backgroundColor: colors[p] }}>
                  {priorityLabel(p)}
                </span>
              </div>
            ))}
          </div>
        </div>

        <div className="flex justify-end gap-2 pt-2 border-t">
          <button onClick={onClose} className="text-sm px-3 py-1.5 rounded bg-gray-100 text-gray-600 hover:bg-gray-200">
            {t('KanbanCancel')}
          </button>
          <button onClick={handleSave} disabled={saving}
            className="text-sm px-3 py-1.5 rounded bg-blue-500 text-white hover:bg-blue-600 disabled:opacity-50">
            {saving ? t('KanbanSaving') : t('KanbanSave')}
          </button>
        </div>
      </div>
    </div>
  );
}
