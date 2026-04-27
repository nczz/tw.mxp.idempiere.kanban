import { useState, useRef, useEffect } from 'react';
import { kanbanFetch } from '../api';

interface Props {
  value: string;
  onChange: (v: string) => void;
  onSubmit: () => void;
  placeholder?: string;
  disabled?: boolean;
}

interface User { id: number; name: string; }

export function MentionInput({ value, onChange, onSubmit, placeholder, disabled }: Props) {
  const [mentionQuery, setMentionQuery] = useState<string | null>(null);
  const [mentionIdx, setMentionIdx] = useState(0);
  const [users, setUsers] = useState<User[]>([]);
  const [cursorPos, setCursorPos] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  // Search users when mentionQuery changes
  useEffect(() => {
    if (mentionQuery === null || mentionQuery.length < 1) { setUsers([]); return; }
    const timer = setTimeout(async () => {
      try {
        const res = await kanbanFetch<{ results: User[] }>(`/lookup?table=AD_User&search=${encodeURIComponent(mentionQuery)}&limit=5`);
        setUsers(res.results || []);
        setMentionIdx(0);
      } catch { setUsers([]); }
    }, 200);
    return () => clearTimeout(timer);
  }, [mentionQuery]);

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const v = e.target.value;
    const pos = e.target.selectionStart || 0;
    onChange(v);
    setCursorPos(pos);

    // Detect @mention trigger
    const before = v.substring(0, pos);
    const atMatch = before.match(/@(\w*)$/);
    if (atMatch) {
      setMentionQuery(atMatch[1]);
    } else {
      setMentionQuery(null);
      setUsers([]);
    }
  }

  function selectUser(user: User) {
    const before = value.substring(0, cursorPos);
    const atIdx = before.lastIndexOf('@');
    const after = value.substring(cursorPos);
    const newValue = before.substring(0, atIdx) + '@' + user.name + ' ' + after;
    onChange(newValue);
    setMentionQuery(null);
    setUsers([]);
    inputRef.current?.focus();
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (users.length > 0 && mentionQuery !== null) {
      if (e.key === 'ArrowDown') { e.preventDefault(); setMentionIdx((i) => Math.min(i + 1, users.length - 1)); return; }
      if (e.key === 'ArrowUp') { e.preventDefault(); setMentionIdx((i) => Math.max(i - 1, 0)); return; }
      if (e.key === 'Enter' || e.key === 'Tab') { e.preventDefault(); selectUser(users[mentionIdx]); return; }
      if (e.key === 'Escape') { setMentionQuery(null); setUsers([]); return; }
    }
    if (e.key === 'Enter' && !e.nativeEvent.isComposing && mentionQuery === null) {
      onSubmit();
    }
  }

  return (
    <div className="relative flex-1">
      <input ref={inputRef} value={value} onChange={handleChange} onKeyDown={handleKeyDown}
        placeholder={placeholder} disabled={disabled}
        className="w-full border rounded px-2 py-1 text-sm" />
      {users.length > 0 && (
        <div className="absolute bottom-full left-0 mb-1 w-full bg-white border rounded shadow-lg z-10 max-h-32 overflow-y-auto">
          {users.map((u, i) => (
            <div key={u.id} onClick={() => selectUser(u)}
              className={`px-3 py-1.5 text-sm cursor-pointer ${i === mentionIdx ? 'bg-blue-50 text-blue-700' : 'text-gray-700 hover:bg-gray-50'}`}>
              @{u.name}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
