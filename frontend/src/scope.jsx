import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { api } from './api.js';

const ScopeContext = createContext(null);
const KEY = 'reservo.restaurantId';

/** Which restaurant the staff/admin area is currently working on (persisted per browser). */
export function ScopeProvider({ children }) {
  const [restaurants, setRestaurants] = useState([]);
  const [restaurantId, setId] = useState(() => {
    try { return localStorage.getItem(KEY) || ''; } catch { return ''; }
  });
  const [loaded, setLoaded] = useState(false);

  const reload = () =>
    api('/api/restaurants', { params: { size: 50 } })
      .then((d) => setRestaurants(d.content))
      .catch(() => {})
      .finally(() => setLoaded(true));

  useEffect(() => { reload(); }, []);

  // fall back to the first restaurant when nothing (valid) is selected
  useEffect(() => {
    if (!loaded || restaurants.length === 0) return;
    if (!restaurants.some((r) => String(r.id) === String(restaurantId))) setId(String(restaurants[0].id));
  }, [loaded, restaurants, restaurantId]);

  const setRestaurantId = (id) => {
    setId(String(id));
    try { localStorage.setItem(KEY, String(id)); } catch { /* storage unavailable */ }
  };

  const value = useMemo(
    () => ({
      restaurants,
      loaded,
      reload,
      restaurantId,
      setRestaurantId,
      restaurant: restaurants.find((r) => String(r.id) === String(restaurantId)) || null,
    }),
    [restaurants, loaded, restaurantId],
  );
  return <ScopeContext.Provider value={value}>{children}</ScopeContext.Provider>;
}

export const useScope = () => useContext(ScopeContext);
