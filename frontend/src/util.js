export const greeting = () => {
  const h = new Date().getHours();
  return h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening';
};

/** "19:30[:00]" -> minutes since midnight */
export const toMin = (t) => {
  const [h, m] = String(t).split(':');
  return Number(h) * 60 + Number(m);
};
export const nowMin = () => {
  const d = new Date();
  return d.getHours() * 60 + d.getMinutes();
};
export const minToHHMM = (m) => `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`;

export const fmtDate = (iso, opts = { month: 'long', day: 'numeric', year: 'numeric' }) =>
  iso ? new Date(`${iso}T00:00:00`).toLocaleDateString(undefined, opts) : '';

export const shiftDate = (iso, days) => {
  const d = new Date(`${iso}T00:00:00`);
  d.setDate(d.getDate() + days);
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
};

export const initials = (name = '') =>
  name.split(/\s+/).filter(Boolean).slice(0, 2).map((s) => s[0].toUpperCase()).join('') || '?';

/**
 * Live status of each table from today's reservations:
 * occupied (seated), reserved (confirmed), pending (awaiting confirmation) or available.
 */
export function tableStatuses(tables, todaysReservations) {
  const now = nowMin();
  const out = new Map();
  for (const t of tables) {
    const mine = todaysReservations.filter((r) => r.tableId === t.id);
    const seated = mine.find((r) => r.status === 'SEATED');
    const upcoming = mine
      .filter((r) => (r.status === 'CONFIRMED' || r.status === 'PENDING') && toMin(r.endTime) > now)
      .sort((a, b) => toMin(a.startTime) - toMin(b.startTime))[0];
    if (seated) out.set(t.id, { status: 'occupied', reservation: seated });
    else if (upcoming) out.set(t.id, { status: upcoming.status === 'CONFIRMED' ? 'reserved' : 'pending', reservation: upcoming });
    else out.set(t.id, { status: 'available', reservation: null });
  }
  return out;
}

export const STATUS_LABEL = {
  available: 'Available',
  occupied: 'Occupied',
  reserved: 'Reserved',
  pending: 'Pending',
};
