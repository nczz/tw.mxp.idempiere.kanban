const contextPath = '/' + window.location.pathname.split('/')[1]; // → "/kanban"

let token = window.location.hash.replace('#token=', '');

/** Listen for token refresh from ZK parent */
window.addEventListener('message', (e) => {
  if (e.data?.type === 'token-refreshed' && e.data.token) {
    token = e.data.token;
  }
});

export async function kanbanFetch<T = unknown>(path: string, opts?: RequestInit): Promise<T> {
  const res = await fetch(contextPath + path, {
    ...opts,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...opts?.headers,
    },
  });

  if (res.status === 401) {
    window.parent.postMessage({ type: 'refresh-token' }, '*');
    throw new Error('Authentication required. Please reopen the form.');
  }

  if (!res.ok) {
    const body = await res.json().catch(() => ({ error: res.statusText }));
    throw new Error(body.error || `HTTP ${res.status}`);
  }

  return res.json();
}

export function hasToken() {
  return token.length > 0;
}

/** Download a file with JWT auth → triggers browser download */
export function downloadFile(path: string, fileName: string) {
  fetch(contextPath + path, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
    .then((res) => { if (!res.ok) throw new Error('Download failed'); return res.blob(); })
    .then((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName;
      a.click();
      URL.revokeObjectURL(url);
    });
}

/** Trigger zoom in iDempiere ZK desktop */
export function zoomRecord(tableName: string, recordId: number) {
  window.parent.postMessage({ type: 'zoom', tableName, recordId: String(recordId) }, '*');
}
