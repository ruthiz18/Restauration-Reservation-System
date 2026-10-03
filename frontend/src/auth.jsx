import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, tokenStore } from './api.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(!!tokenStore.get());

  const logout = useCallback(() => {
    tokenStore.clear();
    setUser(null);
  }, []);

  const loginWithToken = useCallback(async (token, knownUser) => {
    tokenStore.set(token);
    const u = knownUser || (await api('/api/auth/me'));
    setUser(u);
    return u;
  }, []);

  useEffect(() => {
    if (!tokenStore.get()) return;
    api('/api/auth/me')
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const onUnauthorized = () => setUser(null);
    window.addEventListener('reservo:unauthorized', onUnauthorized);
    return () => window.removeEventListener('reservo:unauthorized', onUnauthorized);
  }, []);

  const value = useMemo(
    () => ({
      user,
      loading,
      loginWithToken,
      logout,
      isStaff: user?.role === 'STAFF' || user?.role === 'ADMIN',
      isAdmin: user?.role === 'ADMIN',
    }),
    [user, loading, loginWithToken, logout],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
