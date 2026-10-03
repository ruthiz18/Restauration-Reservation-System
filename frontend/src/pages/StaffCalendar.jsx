import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { NEXT_STATES, api, hhmm, today } from '../api.js';
import { Alert, EmptyState, PageHead, Pager, Spinner, StatusBadge, errorText } from '../components.jsx';
import { useDayData } from '../hooks.js';
import Icon from '../icons.jsx';
import { useScope } from '../scope.jsx';
import { fmtDate, minToHHMM, shiftDate, toMin } from '../util.js';

const PX_PER_MIN = 1; // 1px per minute => a 90 minute booking is a 90px block
const SIZE = 15;
const STATUSES = Object.keys(NEXT_STATES);

export default function StaffCalendar() {
  const { restaurant, restaurantId } = useScope();
  const [date, setDate] = useState(today());
  const [view, setView] = useState('day');

  return (
    <>
      <PageHead title="Reservations">
        <div className="date-nav">
          <button className="icon-btn" aria-label="Previous day" onClick={() => setDate(shiftDate(date, -1))}><Icon name="left" /></button>
          <input type="date" value={date} onChange={(e) => e.target.value && setDate(e.target.value)} aria-label="Date" />
          <button className="icon-btn" aria-label="Next day" onClick={() => setDate(shiftDate(date, 1))}><Icon name="right" /></button>
        </div>
        <button className="btn btn-outline btn-sm" onClick={() => setDate(today())}>Today</button>
        <div className="seg" role="tablist">
          <button className={view === 'day' ? 'on' : ''} onClick={() => setView('day')}>Day</button>
          <button className={view === 'list' ? 'on' : ''} onClick={() => setView('list')}>List</button>
        </div>
      </PageHead>
      {!restaurantId ? <EmptyState icon="store" title="No restaurant selected" />
        : view === 'day' ? <DayGrid restaurant={restaurant} restaurantId={restaurantId} date={date} />
          : <ListView restaurantId={restaurantId} date={date} />}
    </>
  );
}

function DayGrid({ restaurant, restaurantId, date }) {
  const { tables, reservations, loading, error } = useDayData(restaurantId, date);
  const navigate = useNavigate();
  if (!restaurant) return <Spinner />;

  const open = toMin(restaurant.openTime);
  const close = toMin(restaurant.closeTime);
  const height = (close - open) * PX_PER_MIN;
  const hours = [];
  for (let m = Math.ceil(open / 60) * 60; m < close; m += 60) hours.push(m);
  const visible = reservations.filter((r) => r.status !== 'CANCELLED' && r.status !== 'NO_SHOW');

  return (
    <>
      <p className="muted">{fmtDate(date, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })} &middot; {visible.length} reservation{visible.length === 1 ? '' : 's'}</p>
      <Alert kind="error">{error}</Alert>
      {loading ? <Spinner /> : tables.length === 0 ? <EmptyState icon="table" title="No tables configured" /> : (
        <div className="cal card">
          <div className="cal-scroll">
            <div className="cal-grid" style={{ gridTemplateColumns: `64px repeat(${tables.length}, minmax(130px, 1fr))` }}>
              <div className="cal-corner">Time</div>
              {tables.map((t) => (
                <div className="cal-head" key={t.id}><b>{t.label}</b><small>({t.capacity} seats)</small></div>
              ))}

              <div className="cal-times" style={{ height }}>
                {hours.map((m) => <span key={m} style={{ top: (m - open) * PX_PER_MIN }}>{minToHHMM(m)}</span>)}
              </div>
              {tables.map((t) => (
                <div className="cal-col" key={t.id} style={{ height }}>
                  {hours.map((m) => <i key={m} className="cal-line" style={{ top: (m - open) * PX_PER_MIN }} />)}
                  {visible.filter((r) => r.tableId === t.id).map((r) => (
                    <button key={r.id} className={`cal-block b-${r.status}`}
                      style={{ top: (toMin(r.startTime) - open) * PX_PER_MIN, height: (toMin(r.endTime) - toMin(r.startTime)) * PX_PER_MIN - 2 }}
                      onClick={() => navigate(`/staff/reservations/${r.id}`)}
                      title={`${r.customerName} · ${hhmm(r.startTime)}–${hhmm(r.endTime)} · ${r.status}`}>
                      <strong>{r.customerName}</strong>
                      <span>{r.partySize} guests</span>
                      <span>{hhmm(r.startTime)}</span>
                    </button>
                  ))}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
      <ul className="legend">
        {['PENDING', 'CONFIRMED', 'SEATED', 'COMPLETED'].map((s) => <li key={s}><i className={`dot sq b-${s}`} />{s[0] + s.slice(1).toLowerCase()}</li>)}
      </ul>
    </>
  );
}

function ListView({ restaurantId, date }) {
  const navigate = useNavigate();
  const [status, setStatus] = useState('');
  const [allDates, setAllDates] = useState(false);
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    api('/api/reservations', { params: { restaurantId, date: allDates ? '' : date, status, page, size: SIZE } })
      .then((d) => { setData(d); setError(''); })
      .catch((e) => setError(errorText(e)));
  }, [restaurantId, date, allDates, status, page]);
  useEffect(load, [load]);
  useEffect(() => setPage(0), [restaurantId, date, allDates, status]);

  const move = async (id, s) => {
    try {
      await api(`/api/reservations/${id}/status`, { method: 'PATCH', body: { status: s } });
      load();
    } catch (e) {
      setError(errorText(e));
    }
  };

  return (
    <>
      <div className="filters">
        <label className="field inline-field"><span>Status</span>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All</option>
            {STATUSES.map((s) => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
          </select>
        </label>
        <label className="check"><input type="checkbox" checked={allDates} onChange={(e) => setAllDates(e.target.checked)} /> All dates</label>
      </div>
      <Alert kind="error">{error}</Alert>
      {!data && !error && <Spinner />}
      {data && data.content.length === 0 && <EmptyState icon="calendar" title="No reservations match" />}
      {data && data.content.length > 0 && (
        <div className="table-wrap card">
          <table className="tbl">
            <thead><tr><th>#</th><th>Date</th><th>Time</th><th>Guest</th><th>Guests</th><th>Table</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {data.content.map((r) => (
                <tr key={r.id}>
                  <td className="click" onClick={() => navigate(`/staff/reservations/${r.id}`)}>#{r.id}</td>
                  <td>{r.date}</td><td>{hhmm(r.startTime)}</td>
                  <td>{r.customerName}<br /><small className="muted">{r.customerEmail}</small></td>
                  <td>{r.partySize}</td><td>{r.tableLabel}</td>
                  <td><StatusBadge status={r.status} /></td>
                  <td>
                    <div className="btn-row tight">
                      {NEXT_STATES[r.status].map((s) => (
                        <button key={s} className={`btn btn-sm ${s === 'CANCELLED' || s === 'NO_SHOW' ? 'btn-danger-outline' : 'btn-primary'}`}
                          onClick={() => move(r.id, s)}>{s.replace('_', ' ')}</button>
                      ))}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data && <Pager page={data.page} size={data.size} total={data.total} onChange={setPage} />}
    </>
  );
}
