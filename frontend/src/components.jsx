import { Link, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import { BRAND } from './brand.js';
import Icon from './icons.jsx';

export function Logo({ light = false, to = '/', size = 'md' }) {
  return (
    <Link to={to} className={`logo logo-${size}${light ? ' logo-light' : ''}`}>
      <Icon name="hat" size={size === 'lg' ? 56 : 30} />
      <span>{BRAND.name}</span>
    </Link>
  );
}

/** Client-side route guard. The server enforces the same rules (RBAC) on every API call. */
export function Protected({ roles, children }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <Spinner />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (roles && !roles.includes(user.role)) {
    return (
      <div className="page">
        <Alert kind="error">You do not have permission to view this page.</Alert>
      </div>
    );
  }
  return children;
}

export const Spinner = () => <p className="muted pad">Loading&hellip;</p>;

export function Alert({ kind = 'info', children }) {
  if (!children) return null;
  return <div className={`alert alert-${kind}`} role={kind === 'error' ? 'alert' : 'status'}>{children}</div>;
}

export const EmptyState = ({ icon = 'calendar', title, children }) => (
  <div className="empty">
    <Icon name={icon} size={34} />
    <strong>{title}</strong>
    {children && <p className="muted">{children}</p>}
  </div>
);

export function StatusBadge({ status }) {
  return <span className={`badge b-${status}`}>{status.replace('_', ' ')}</span>;
}

export function Pager({ page, size, total, onChange }) {
  const pages = Math.max(1, Math.ceil(total / size));
  if (pages <= 1) return null;
  return (
    <div className="pager">
      <button className="btn btn-outline btn-sm" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        <Icon name="left" size={16} /> Previous
      </button>
      <span className="muted">Page {page + 1} of {pages}</span>
      <button className="btn btn-outline btn-sm" disabled={page + 1 >= pages} onClick={() => onChange(page + 1)}>
        Next <Icon name="right" size={16} />
      </button>
    </div>
  );
}

export function PageHead({ title, sub, children }) {
  return (
    <div className="page-head">
      <div>
        <h1>{title}</h1>
        {sub && <p className="muted">{sub}</p>}
      </div>
      {children && <div className="page-actions">{children}</div>}
    </div>
  );
}

export function StatCard({ label, value, icon, tone = 'orange' }) {
  return (
    <div className="stat-card">
      <div>
        <span className="stat-label">{label}</span>
        <strong className="stat-value">{value}</strong>
      </div>
      <span className={`chip chip-${tone}`}><Icon name={icon} size={24} /></span>
    </div>
  );
}

/** Pending -> Confirmed -> Seated -> Completed, with the cancel / no-show exits. Mirrors the backend state machine. */
export function StatusFlow({ status }) {
  const main = [
    { key: 'PENDING', label: 'Pending', icon: 'clock' },
    { key: 'CONFIRMED', label: 'Confirmed', icon: 'check' },
    { key: 'SEATED', label: 'Seated', icon: 'users' },
    { key: 'COMPLETED', label: 'Completed', icon: 'check' },
  ];
  const idx = main.findIndex((m) => m.key === status);
  const terminal = status === 'CANCELLED' || status === 'NO_SHOW';
  return (
    <div className="flow">
      <div className="flow-main">
        {main.map((m, i) => (
          <div className="flow-item" key={m.key}>
            <div className={`flow-node n-${m.key}${i === idx ? ' current' : ''}${idx > i ? ' done' : ''}${terminal ? ' dim' : ''}`}>
              <Icon name={m.icon} size={26} />
            </div>
            <span>{m.label}</span>
            {i < main.length - 1 && <Icon name="arrow" size={18} className="flow-arrow" />}
          </div>
        ))}
      </div>
      <div className="flow-exits">
        <div className="flow-item">
          <div className={`flow-node n-CANCELLED${status === 'CANCELLED' ? ' current' : ' dim'}`}><Icon name="x" size={22} /></div>
          <span>Cancelled</span>
        </div>
        <div className="flow-item">
          <div className={`flow-node n-NO_SHOW${status === 'NO_SHOW' ? ' current' : ' dim'}`}><Icon name="user" size={22} /></div>
          <span>No show</span>
        </div>
      </div>
    </div>
  );
}

/** Turns an ApiError (with optional per-field messages) into a readable string. */
export function errorText(e) {
  if (!e) return '';
  const fields = e.fields ? Object.entries(e.fields).map(([k, v]) => `${k}: ${v}`).join('; ') : '';
  return fields ? `${e.message} (${fields})` : e.message;
}
