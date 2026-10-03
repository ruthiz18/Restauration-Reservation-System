import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Alert, errorText } from '../components.jsx';
import { AuthLayout } from '../layouts.jsx';

export default function Register() {
  const { loginWithToken } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const res = await api('/api/auth/register', { method: 'POST', body: { ...form, phone: form.phone || null } });
      await loginWithToken(res.token, res.user);
      navigate('/', { replace: true });
    } catch (err) {
      setError(errorText(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthLayout>
      <h1>Create your account</h1>
      <p className="muted">Book tables in seconds and get confirmations by email and SMS.</p>
      <Alert kind="error">{error}</Alert>
      <form onSubmit={submit}>
        <label className="field"><span>Full name</span>
          <input required maxLength="120" value={form.fullName} onChange={set('fullName')} /></label>
        <label className="field"><span>Email address</span>
          <input type="email" required autoComplete="email" value={form.email} onChange={set('email')} /></label>
        <label className="field"><span>Phone (for SMS reminders)</span>
          <input type="tel" maxLength="30" placeholder="+250 7xx xxx xxx" value={form.phone} onChange={set('phone')} /></label>
        <label className="field"><span>Password (8+ characters)</span>
          <input type="password" required minLength="8" maxLength="72" autoComplete="new-password" value={form.password} onChange={set('password')} /></label>
        <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Creating…' : 'Create account'}</button>
      </form>
      <p className="muted center">Already registered? <Link to="/login">Sign in</Link></p>
    </AuthLayout>
  );
}
