import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { NEXT_STATES, api, hhmm } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Alert, Spinner, StatusBadge, StatusFlow, errorText } from '../components.jsx';
import Icon from '../icons.jsx';
import { fmtDate } from '../util.js';

const ACTION_LABEL = {
  CONFIRMED: 'Confirm',
  SEATED: 'Mark as Seated',
  COMPLETED: 'Mark as Completed',
  NO_SHOW: 'Mark No-show',
  CANCELLED: 'Cancel Reservation',
};

/** Shared by customers (their own booking) and staff (any booking). The server enforces who may do what. */
export default function ReservationDetail() {
  const { id } = useParams();
  const { isStaff } = useAuth();
  const [r, setR] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    api(`/api/reservations/${id}`).then(setR).catch((e) => setError(errorText(e)));
  }, [id]);
  useEffect(load, [load]);

  const move = async (status) => {
    if (status === 'CANCELLED' && !window.confirm('Cancel this reservation?')) return;
    try {
      setR(await api(`/api/reservations/${id}/status`, { method: 'PATCH', body: { status } }));
      setError('');
    } catch (e) {
      setError(errorText(e));
    }
  };

  const back = isStaff ? '/staff/reservations' : '/reservations';
  if (!r) return <div className={isStaff ? '' : 'container'}>{error ? <Alert kind="error">{error}</Alert> : <Spinner />}</div>;

  // customers may only cancel; staff get every legal transition
  const actions = NEXT_STATES[r.status].filter((s) => isStaff || s === 'CANCELLED');

  return (
    <div className={isStaff ? '' : 'container narrow'}>
      <Link to={back} className="back-link"><Icon name="left" size={16} /> Back to reservations</Link>
      <div className="page-head">
        <div>
          <h1>Reservation #{r.id}</h1>
          <p className="muted">{r.restaurantName}</p>
        </div>
        <StatusBadge status={r.status} />
      </div>
      <Alert kind="error">{error}</Alert>

      <div className="detail-grid">
        <div className="card card-pad">
          <h3>Booking</h3>
          <dl className="detail-list">
            <div><dt><Icon name="users" size={16} /> Guests</dt><dd>{r.partySize}</dd></div>
            <div><dt><Icon name="calendar" size={16} /> Date</dt><dd>{fmtDate(r.date)}</dd></div>
            <div><dt><Icon name="clock" size={16} /> Time</dt><dd>{hhmm(r.startTime)} &ndash; {hhmm(r.endTime)}</dd></div>
            <div><dt><Icon name="table" size={16} /> Table</dt><dd>{r.tableLabel}</dd></div>
            {r.specialRequests && <div><dt><Icon name="note" size={16} /> Requests</dt><dd>{r.specialRequests}</dd></div>}
          </dl>
        </div>
        <div className="card card-pad">
          <h3>Customer</h3>
          <dl className="detail-list">
            <div><dt><Icon name="user" size={16} /> Name</dt><dd>{r.customerName}</dd></div>
            <div><dt><Icon name="mail" size={16} /> Email</dt><dd>{r.customerEmail}</dd></div>
          </dl>
        </div>
      </div>

      <div className="card card-pad gap">
        <h3>Reservation actions</h3>
        {actions.length === 0 && <p className="muted">No further actions &mdash; this reservation is {r.status.replace('_', ' ').toLowerCase()}.</p>}
        <div className="btn-row">
          {actions.map((s) => (
            <button key={s} className={`btn ${s === 'CANCELLED' || s === 'NO_SHOW' ? 'btn-danger-outline' : 'btn-primary'}`}
              onClick={() => move(s)}>
              {ACTION_LABEL[s]}
            </button>
          ))}
        </div>
      </div>

      <div className="card card-pad gap">
        <h3>Reservation status flow</h3>
        <StatusFlow status={r.status} />
      </div>
    </div>
  );
}
