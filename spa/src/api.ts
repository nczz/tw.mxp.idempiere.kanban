const contextPath = '/' + window.location.pathname.split('/')[1]; // → "/kanban"

let token = window.location.hash.replace('#token=', '');

/** Listen for token refresh from ZK parent */
window.addEventListener('message', (e) => {
  if (e.data?.type === 'token-refreshed' && e.data.token) {
    token = e.data.token;
  }
});

export function kanbanFetch(path: string, opts?: RequestInit): Promise<Response> {
  return fetch(contextPath + path, {
    ...opts,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...opts?.headers,
    },
  }).then((res) => {
    if (res.status === 401) {
      // Request token refresh from ZK parent
      window.parent.postMessage({ type: 'refresh-token' }, '*');
    }
    return res;
  });
}

export function getToken() {
  return token;
}

/** Trigger zoom in iDempiere ZK desktop */
export function zoomRecord(tableName: string, recordId: number) {
  window.parent.postMessage({ type: 'zoom', tableName, recordId: String(recordId) }, '*');
}
