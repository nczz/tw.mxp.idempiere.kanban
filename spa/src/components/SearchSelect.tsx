import { useState, useEffect, useRef } from 'react';
import { kanbanFetch } from '../api';

interface Props {
  table: string;
  value: number | undefined;
  valueName?: string;
  onChange: (id: number | undefined, name: string) => void;
  label: string;
}

interface LookupItem {
  id: number;
  name: string;
}

export function SearchSelect({ table, value, valueName, onChange, label }: Props) {
  const [search, setSearch] = useState(valueName || '');
  const [results, setResults] = useState<LookupItem[]>([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  // Close on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // Sync display when value changes externally
  useEffect(() => {
    if (!value) setSearch('');
    else if (valueName) setSearch(valueName);
  }, [value, valueName]);

  function doSearch(q: string) {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      setLoading(true);
      try {
        const data = await kanbanFetch<{ results: LookupItem[] }>(
          `/lookup?table=${table}&search=${encodeURIComponent(q)}&limit=15`
        );
        setResults(data.results);
        setOpen(true);
      } catch { setResults([]); }
      setLoading(false);
    }, 300);
  }

  function handleInput(q: string) {
    setSearch(q);
    if (q.length >= 1) doSearch(q);
    else { setResults([]); setOpen(false); }
  }

  function select(item: LookupItem) {
    setSearch(item.name);
    onChange(item.id, item.name);
    setOpen(false);
  }

  function clear() {
    setSearch('');
    onChange(undefined, '');
    setResults([]);
    setOpen(false);
  }

  return (
    <div ref={ref} className="relative">
      <label className="text-xs text-gray-500">{label}</label>
      <div className="flex mt-0.5">
        <input
          value={search}
          onChange={(e) => handleInput(e.target.value)}
          onFocus={() => { if (search.length >= 1) doSearch(search); }}
          placeholder={`Search ${label}...`}
          className="flex-1 border rounded-l px-2 py-1 text-sm"
        />
        {value ? (
          <button onClick={clear} className="border border-l-0 rounded-r px-2 text-xs text-gray-400 hover:text-red-500">✕</button>
        ) : (
          <span className="border border-l-0 rounded-r px-2 text-xs text-gray-300 flex items-center">🔍</span>
        )}
      </div>
      {open && results.length > 0 && (
        <div className="absolute z-10 w-full mt-0.5 bg-white border rounded shadow-lg max-h-48 overflow-y-auto">
          {results.map((item) => (
            <div key={item.id} onClick={() => select(item)}
              className={`px-2 py-1.5 text-sm cursor-pointer hover:bg-blue-50 ${item.id === value ? 'bg-blue-50 font-medium' : ''}`}>
              {item.name}
            </div>
          ))}
        </div>
      )}
      {open && loading && (
        <div className="absolute z-10 w-full mt-0.5 bg-white border rounded shadow-lg px-2 py-2 text-xs text-gray-400">
          Searching...
        </div>
      )}
    </div>
  );
}
