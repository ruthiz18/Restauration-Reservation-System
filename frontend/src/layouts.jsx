import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import { BRAND } from './brand.js';
import { Logo } from './components.jsx';
import Icon from './icons.jsx';
import { ScopeProvider, useScope } from './scope.jsx';
import { sideColors } from './themes.js';
import { initials } from './util.js';

/** Public / customer pages: light top bar. */
export function PublicLayout() {
  const { user, logout, isStaff } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const location = useLocation();
  useEffect(() => setOpen(false), [location.pathname]);

  return (
    <>
      <header className="pub-nav">
        <div className="pub-inner">
          <Logo />
          <button className="icon-btn nav-toggle" aria-label="Toggle menu" aria-expanded={open} onClick={() => setOpen(!open)}>
            <Icon name="menuBars" />
          </button>
          <nav className={`pub-links${open ? ' open' : ''}`}>
            <NavLink to="/" end>Restaurants</NavLink>
            {user && !isStaff && <NavLink to="/reservations">My reservations</NavLink>}
            {isStaff && <NavLink to="/staff">Dashboard</NavLink>}
            {user ? (
              <>
                <span className="avatar" title={user.fullName}>{initials(user.fullName)}</span>
                <button className="btn btn-outline btn-sm" onClick={() => { logout(); navigate('/'); }}>Sign out</button>
              </>
            ) : (
              <>
                <NavLink to="/login">Sign in</NavLink>
                <Link to="/register" className="btn btn-primary btn-sm">Create account</Link>
              </>
            )}
          </nav>
        </div>
      </header>
      <main className="pub-main"><Outlet /></main>
      <footer className="pub-footer">{BRAND.name} &mdash; {BRAND.footer}</footer>
    </>
  );
}

const NAV = [
  { to: '/staff', label: 'Dashboard', icon: 'dashboard', end: true },
  { to: '/staff/reservations', label: 'Reservations', icon: 'calendar' },
  { to: '/staff/tables', label: 'Tables', icon: 'table' },
  { to: '/staff/menu', label: 'Menu', icon: 'menu' },
];
const ADMIN_NAV = [
  { to: '/admin/restaurants', label: 'Restaurants', icon: 'store' },
  { to: '/admin/users', label: 'Users', icon: 'users' },
  { to: '/admin/notifications', label: 'Notifications', icon: 'mail' },
];

/** Staff / admin area: dark sidebar + top bar, like the design mock-ups. */
export function StaffLayout() {
  return (
    <ScopeProvider>
      <StaffShell />
    </ScopeProvider>
  );
}

function StaffShell() {
  const { user, logout, isAdmin } = useAuth();
  const { restaurants, restaurantId, setRestaurantId, restaurant } = useScope();
  const [sideFrom, sideTo] = sideColors(restaurant?.cuisine);
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  useEffect(() => setOpen(false), [location.pathname]);

  const item = (n) => (
    <NavLink key={n.to} to={n.to} end={n.end} className="side-link">
      <Icon name={n.icon} size={19} /> {n.label}
    </NavLink>
  );

  return (
    <div className="shell" style={{ '--side-from': sideFrom, '--side-to': sideTo }}>
      <aside className={`sidebar${open ? ' open' : ''}`}>
        <div className="side-brand">
          <Logo light to="/staff" />
          <small>{BRAND.tagline}</small>
        </div>
        <nav className="side-nav">
          {NAV.map(item)}
          {isAdmin && <div className="side-section">Administration</div>}
          {isAdmin && ADMIN_NAV.map(item)}
        </nav>
        <div className="side-foot">
          <Link to="/" className="side-link"><Icon name="eye" size={19} /> View public site</Link>
          <button className="side-link" onClick={() => { logout(); navigate('/'); }}>
            <Icon name="logout" size={19} /> Logout
          </button>
        </div>
      </aside>
      {open && <div className="scrim" onClick={() => setOpen(false)} />}

      <div className="main">
        <header className="topbar">
          <button className="icon-btn side-toggle" aria-label="Open menu" onClick={() => setOpen(true)}>
            <Icon name="menuBars" />
          </button>
          <div className="scope-select">
            <Icon name="store" size={18} />
            <select aria-label="Restaurant" value={restaurantId} onChange={(e) => setRestaurantId(e.target.value)}>
              {restaurants.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
            </select>
          </div>
          <div className="topbar-user">
            <span className="avatar">{initials(user.fullName)}</span>
            <span className="who">
              <strong>{user.fullName}</strong>
              <small>{user.role.charAt(0) + user.role.slice(1).toLowerCase()}</small>
            </span>
          </div>
        </header>
        <div className="page"><Outlet /></div>
      </div>
    </div>
  );
}

/** Login / register: dark hero on the left, form on the right. */
export function AuthLayout({ children }) {
  return (
    <div className="auth">
      <section className="auth-hero">
        <div className="auth-hero-inner">
          <Logo light size="lg" />
          <p>{BRAND.tagline}</p>
        </div>
        <div className="auth-blob b1" /><div className="auth-blob b2" />
      </section>
      <section className="auth-panel">
        <div className="auth-box">{children}</div>
        <p className="auth-foot">{BRAND.footer}<br />{BRAND.name}</p>
      </section>
    </div>
  );
}
