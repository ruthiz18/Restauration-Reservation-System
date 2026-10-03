import { Link, useNavigate } from 'react-router-dom';
import { hhmm, today } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Alert, EmptyState, PageHead, Spinner, StatCard, StatusBadge } from '../components.jsx';
import { useDayData } from '../hooks.js';
import Icon from '../icons.jsx';
import { useScope } from '../scope.jsx';
import { themeStyle } from '../themes.js';
import { STATUS_LABEL, fmtDate, greeting, tableStatuses, toMin } from '../util.js';

const day = today();

export default function StaffDashboard() {
  const { user } = useAuth();
  const { restaurant, restaurantId, loaded } = useScope();
  const { tables, reservations, loading, error } = useDayData(restaurantId, day);
  const navigate = useNavigate();

  if (loaded && !restaurantId) return <EmptyState icon="store" title="No restaurants yet">An admin needs to add one first.</EmptyState>;

  const live = reservations.filter((r) => r.status !== 'CANCELLED' && r.status !== 'NO_SHOW');
  const statuses = tableStatuses(tables, reservations);
  const count = (s) => [...statuses.values()].filter((v) => v.status === s).length;
  const sorted = [...live].sort((a, b) => toMin(a.startTime) - toMin(b.startTime));

  return (
    <>
      <PageHead title={`${greeting()}, ${user.fullName.split(' ')[0]} 👋`}
        sub={`Here's what's happening at ${restaurant?.name ?? 'your restaurant'} today (${fmtDate(day)}).`} />
      {restaurant && (
        <div className="banner" style={themeStyle(restaurant.cuisine)}>
          <div>
            <em className="tag">{restaurant.cuisine}</em>
            <h2>{restaurant.name}</h2>
            <p><Icon name="pin" size={15} /> {restaurant.address} &middot; <Icon name="clock" size={15} /> {hhmm(restaurant.openTime)}&ndash;{hhmm(restaurant.closeTime)}</p>
          </div>
        </div>
      )}
      <Alert kind="error">{error}</Alert>
      {loading ? <Spinner /> : (
        <>
          <div className="stat-grid">
            <StatCard label="Today's reservations" value={live.length} icon="calendar" tone="orange" />
            <StatCard label="Available tables" value={count('available')} icon="table" tone="green" />
            <StatCard label="Occupied tables" value={count('occupied')} icon="users" tone="blue" />
            <StatCard label="Pending reservations" value={reservations.filter((r) => r.status === 'PENDING').length} icon="clock" tone="amber" />
          </div>

          <div className="panel-grid">
            <section className="card card-pad">
              <div className="card-head">
                <h3>Today&rsquo;s reservations</h3>
                <Link to="/staff/reservations" className="link-sm">View all <Icon name="arrow" size={14} /></Link>
              </div>
              {sorted.length === 0 ? <p className="muted">No reservations today.</p> : (
                <div className="table-wrap flat">
                  <table className="tbl">
                    <thead><tr><th>Time</th><th>Customer</th><th>Table</th><th>Guests</th><th>Status</th></tr></thead>
                    <tbody>
                      {sorted.map((r) => (
                        <tr key={r.id} className="click" onClick={() => navigate(`/staff/reservations/${r.id}`)}>
                          <td>{hhmm(r.startTime)}</td><td>{r.customerName}</td><td>{r.tableLabel}</td>
                          <td>{r.partySize}</td><td><StatusBadge status={r.status} /></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>

            <section className="card card-pad">
              <div className="card-head">
                <h3>Table status</h3>
                <Link to="/staff/tables" className="link-sm">View all <Icon name="arrow" size={14} /></Link>
              </div>
              {tables.length === 0 ? <p className="muted">No tables configured.</p> : (
                <div className="mini-tiles">
                  {tables.map((t) => {
                    const st = statuses.get(t.id).status;
                    return (
                      <Link to="/staff/tables" key={t.id} className={`mini-tile t-${st}`} title={`${t.label}: ${STATUS_LABEL[st]}`}>
                        <Icon name="table" size={22} /><span>{t.label}</span>
                      </Link>
                    );
                  })}
                </div>
              )}
              <ul className="legend">
                {Object.entries(STATUS_LABEL).map(([k, v]) => <li key={k}><i className={`dot t-${k}`} />{v}</li>)}
              </ul>
            </section>
          </div>
        </>
      )}
    </>
  );
}
