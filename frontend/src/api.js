const BASE = import.meta.env.VITE_API_URL || '';
export const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
const TOKEN_KEY = 'reservo.token';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

export class ApiError extends Error {
  constructor(status, message, fields) {
    super(message);
    this.status = status;
    this.fields = fields || null;
  }
}

/** Thin fetch wrapper: JSON in/out, attaches the JWT, normalises the backend's error body. */
export async function api(path, { method = 'GET', body, params } = {}) {
  const url = new URL(BASE + path, window.location.origin);
  if (params) {
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, v);
    });
  }
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = tokenStore.get();
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(url, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Is the backend running?');
  }
  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    if (res.status === 401 && token) {
      tokenStore.clear();
      window.dispatchEvent(new Event('reservo:unauthorized'));
    }
    throw new ApiError(res.status, data?.message || `Request failed (${res.status})`, data?.errors);
  }
  return data;
}

export const hhmm = (t) => (t ? String(t).slice(0, 5) : '');

export const today = () => {
  const d = new Date();
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
};

// Mirrors ReservationStatus.nextStates() on the backend (the server remains the authority).
export const NEXT_STATES = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['SEATED', 'CANCELLED', 'NO_SHOW'],
  SEATED: ['COMPLETED'],
  COMPLETED: [],
  CANCELLED: [],
  NO_SHOW: [],
};
