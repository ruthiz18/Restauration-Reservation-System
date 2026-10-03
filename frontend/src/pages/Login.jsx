import { useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { BACKEND_URL, api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Alert, errorText } from '../components.jsx';
import Icon from '../icons.jsx';
import { AuthLayout } from '../layouts.jsx';

const OAUTH_ERRORS = {
  oauth_email: 'Your Google account did not share an email address.',
  disabled: 'This account has been disabled. Contact an administrator.',
};

export default function Login() {
  const { loginWithToken } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [params] = useSearchParams();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [show, setShow] = useState(false);
  const [error, setError] = useState(OAUTH_ERRORS[params.get('error')] || '');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const res = await api('/api/auth/login', { method: 'POST', body: { email, password } });
      const user = await loginWithToken(res.token, res.user);
      navigate(location.state?.from || (user.role === 'CUSTOMER' ? '/' : '/staff'), { replace: true });
    } catch (err) {
      setError(err.status === 401 ? 'Invalid email or password.' : errorText(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthLayout>
      <h1>Welcome Back!</h1>
      <p className="muted">Sign in to manage your reservations.</p>
      <Alert kind="error">{error}</Alert>
      <form onSubmit={submit}>
        <label className="field">
          <span>Email address</span>
          <input type="email" required autoComplete="email" placeholder="you@example.com" value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label className="field">
          <span>Password</span>
          <div className="input-wrap">
            <input type={show ? 'text' : 'password'} required autoComplete="current-password" placeholder="Enter your password"
              value={password} onChange={(e) => setPassword(e.target.value)} />
            <button type="button" className="icon-btn" aria-label={show ? 'Hide password' : 'Show password'} onClick={() => setShow(!show)}>
              <Icon name={show ? 'eyeOff' : 'eye'} size={18} />
            </button>
          </div>
        </label>
        <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Signing in…' : 'Sign In'}</button>
      </form>
      <div className="divider">or</div>
      {/* OAuth2 authorization-code flow is handled by Spring Security on the backend. */}
      <a className="btn btn-outline btn-block" href={`${BACKEND_URL}/oauth2/authorization/google`}>Continue with Google</a>
      <p className="muted center">New here? <Link to="/register">Create an account</Link> · <Link to="/">Browse restaurants</Link></p>
    </AuthLayout>
  );
}
