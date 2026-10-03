import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, hhmm } from '../api.js';
import { Alert, EmptyState, PageHead, Pager, Spinner, StatusBadge, errorText } from '../components.jsx';
import Icon from '../icons.jsx';
import { fmtDate } from '../util.js';

const SIZE = 10;

export default function MyReservations() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    api('/api/reservations/mine', { params: { page, size: SIZE } })
      .then((d) => { setData(d); setError(''); })
      .catch((e) => setError(errorText(e)));
  }, [page]);

  useEffect(load, [load]);

  const cancel = async (id) => {
    if (!window.confirm('Cancel this reservation?')) return;
    try {
      await api(`/api/reservations/${id}/status`, { method: 'PATCH', body: { status: 'CANCELLED' } });
      load();
    } catch (e) {
      setError(errorText(e));
    }
  };

  return (
    <div className="container">
      <PageHead title="My reservations" sub="Upcoming and past bookings">
        <Link to="/" className="btn btn-primary"><Icon name="plus" size={16} /> New reservation</Link>
      </PageHead>
      <Alert kind="error">{error}</Alert>
      {!data && !error && <Spinner />}
      {data && data.content.length === 0 && (
        <EmptyState icon="calendar" title="No reservations yet">
          <Link to="/">Find a restaurant</Link> to make your first booking.
        </EmptyState>
      )}
      <div className="res-list">
        {data?.content.map((r) => (
          <article className="res-card" key={r.id}>
            <div className="res-date">
              <b>{new Date(`${r.date}T00:00:00`).getDate()}</b>
              <span>{fmtDate(r.date, { month: 'short' })}</span>
            </div>
            <div className="res-main">
              <h3><Link to={`/restaurants/${r.restaurantId}`}>{r.restaurantName}</Link></h3>
              <p className="meta">
                <span><Icon name="clock" size={15} /> {hhmm(r.startTime)}&ndash;{hhmm(r.endTime)}</span>
                <span><Icon name="users" size={15} /> {r.partySize} guests</span>
                <span><Icon name="table" size={15} /> {r.tableLabel}</span>
              </p>
            </div>
            <StatusBadge status={r.status} />
            <div className="res-actions">
              <Link className="btn btn-outline btn-sm" to={`/reservations/${r.id}`}>Details</Link>
              {(r.status === 'PENDING' || r.status === 'CONFIRMED') && (
                <button className="btn btn-danger btn-sm" onClick={() => cancel(r.id)}>Cancel</button>
              )}
              {r.status === 'COMPLETED' && (
                <Link className="btn btn-primary btn-sm" to={`/restaurants/${r.restaurantId}`}>Review</Link>
              )}
            </div>
          </article>
        ))}
      </div>
      {data && <Pager page={data.page} size={data.size} total={data.total} onChange={setPage} />}
    </div>
  );
}
