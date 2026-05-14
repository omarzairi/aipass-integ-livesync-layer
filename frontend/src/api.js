const BASE = import.meta.env.VITE_API_URL || '';

async function request(path) {
  const res = await fetch(`${BASE}${path}`);
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json();
}

async function postRequest(path) {
  const res = await fetch(`${BASE}${path}`, { method: 'POST' });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json();
}

export function fetchHealth() {
  return request('/api/health');
}

export function fetchEvents(page = 0, size = 20) {
  return request(`/api/events?page=${page}&size=${size}`);
}

export function fetchEvent(id) {
  return request(`/api/events/${id}`);
}

export function fetchLogs(eventId, page = 0, size = 50) {
  const params = eventId ? `eventId=${eventId}&` : '';
  return request(`/api/logs?${params}page=${page}&size=${size}`);
}

export function replayEvent(id) {
  return postRequest(`/api/events/${id}/replay`);
}
