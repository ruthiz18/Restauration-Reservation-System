import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { Alert, Spinner, errorText } from '../components.jsx';
import { AuthLayout } from '../layouts.jsx';

/**
 * Landing page for the backend's OAuth2 success redirect: /oauth2/callback#token=<jwt>.
 * The token is in the URL fragment, so it is never sent to any server.
 */
export default function OAuthCallback() {
  const { loginWithToken } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const ran = useRef(false);

  useEffect(() => {
    if (ran.current) return; // StrictMode runs effects twice in dev
    ran.current = true;
    const token = new URLSearchParams(window.location.hash.slice(1)).get('token');
    if (!token) {
      setError('No sign-in token was received.');
      return;
    }
    window.history.replaceState(null, '', window.location.pathname); // drop the token from the address bar
    loginWithToken(token)
      .then((u) => navigate(u.role === 'CUSTOMER' ? '/' : '/staff', { replace: true }))
      .catch((e) => setError(errorText(e)));
  }, [loginWithToken, navigate]);

  return (
    <AuthLayout>
      <h1>Signing you in…</h1>
      {error ? <><Alert kind="error">{error}</Alert><Link to="/login">Back to sign in</Link></> : <Spinner />}
    </AuthLayout>
  );
}
