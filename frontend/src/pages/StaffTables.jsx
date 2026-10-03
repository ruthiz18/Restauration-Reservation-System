import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, hhmm, today } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Alert, EmptyState, PageHead, Spinner, errorText } from '../components.jsx';
import { useDayData } from '../hooks.js';
import Icon from '../icons.jsx';
import { useScope } from '../scope.jsx';
import { STATUS_LABEL, tableStatuses } from '../util.js';

const day = today();
const FILTERS = ['all', 'available', 'reserved', 'occupied', 'pending'];

/** Live floor view: every table with its current status; admins can add / remove tables. */
export default function StaffTables() {
  const { isAdmin } = useAuth();
  const { restaurantId, restaurant } = useScope();
  const { tables, reservations, loading, error, reload } = useDayData(restaurantId, day);
  const [filter, setFilter] = useState('all');
  const [selectedId, setSelectedId] = useState(null);
  const [msg, setMsg] = useState('');
  const [form, setForm] = useState({ label: '', capacity: 2 });

  const statuses = tableStatuses(tables, reservations);
  const count = (f) => (f === 'all' ? tables.length : [...statuses.values()].filter((v) => v.status === f).length);
  const shown = tables.filter((t) => filter === 'all' || statuses.get(t.id).status === filter);
  const selected = tables.find((t) => t.id === selectedId) || tables[0];

  useEffect(() => { setSelectedId(null); }, [restaurantId]);

  const add = async (e) => {
    e.preventDefault();
    try {
      await api(`/api/restaurants/${restaurantId}/tables`, { method: 'POST', body: { label: form.label, capacity: Number(form.capacity) } });
      setForm({ label: '', capacity: 2 });
      setMsg('');
      reload();
    } catch (err) {
      setMsg(errorText(err));
    }
  };

  const remove = async (t) => {
    if (!window.confirm(`Deactivate table ${t.label}?`)) return;
    try {
      await api(`/api/tables/${t.id}`, { method: 'DELETE' });
      setSelectedId(null);
      reload();
    } catch (err) {
      setMsg(errorText(err));
    }
  };

  const sel = selected && statuses.get(selected.id);

  return (
    <>
      <PageHead title="Tables" sub={restaurant ? `${restaurant.name} · live status for today` : ''} />
      <Alert kind="error">{error || msg}</Alert>
      <div className="chips">
        {FILTERS.map((f) => (
          <button key={f} className={`chip-btn${filter === f ? ' on' : ''}`} onClick={() => setFilter(f)}>
            {f === 'all' ? 'All' : STATUS_LABEL[f]} ({count(f)})
          </button>
        ))}
      </div>

      {loading ? <Spinner /> : tables.length === 0 && !isAdmin ? <EmptyState icon="table" title="No tables configured" /> : (
        <div className="panel-grid wide-left">
          <section className="card card-pad">
            <div className="floor">
              {shown.map((t) => {
                const st = statuses.get(t.id).status;
                return (
                  <button key={t.id} className={`tile t-${st}${selected?.id === t.id ? ' picked' : ''}`} onClick={() => setSelectedId(t.id)}>
                    <Icon name="table" size={30} />
                    <b>{t.label}</b>
                    <small>{t.capacity} seats</small>
                    <span className="tile-status">{STATUS_LABEL[st]}</span>
                  </button>
                );
              })}
              {shown.length === 0 && <p className="muted">No tables with this status.</p>}
            </div>
            <ul className="legend">
              {Object.entries(STATUS_LABEL).map(([k, v]) => <li key={k}><i className={`dot t-${k}`} />{v}</li>)}
            </ul>
          </section>

          <aside className="card card-pad">
            <h3>Table details</h3>
            {!selected ? <p className="muted">Select a table.</p> : (
              <>
                <div className="detail-title">
                  <strong>Table {selected.label}</strong>
                  <span className={`pill-status t-${sel.status}`}>{STATUS_LABEL[sel.status]}</span>
                </div>
                <dl className="detail-list">
                  <div><dt>Capacity</dt><dd>{selected.capacity} seats</dd></div>
                  <div><dt>Current reservation</dt>
                    <dd>{sel.reservation
                      ? <Link to={`/staff/reservations/${sel.reservation.id}`}>{sel.reservation.customerName} · {hhmm(sel.reservation.startTime)}</Link>
                      : 'None'}</dd>
                  </div>
                </dl>
                <div className="btn-row">
                  <Link to="/staff/reservations" className="btn btn-outline btn-sm">View schedule</Link>
                  {isAdmin && <button className="btn btn-danger-outline btn-sm" onClick={() => remove(selected)}><Icon name="trash" size={15} /> Deactivate</button>}
                </div>
              </>
            )}

            {isAdmin && (
              <form onSubmit={add} className="add-table">
                <h3>Add a table</h3>
                <div className="grid-2">
                  <label className="field"><span>Label</span>
                    <input required maxLength="30" placeholder="T8" value={form.label} onChange={(e) => setForm({ ...form, label: e.target.value })} /></label>
                  <label className="field"><span>Seats</span>
                    <input type="number" min="1" max="30" value={form.capacity} onChange={(e) => setForm({ ...form, capacity: e.target.value })} /></label>
                </div>
                <button className="btn btn-primary btn-block"><Icon name="plus" size={16} /> Add table</button>
              </form>
            )}
          </aside>
        </div>
      )}
    </>
  );
}
