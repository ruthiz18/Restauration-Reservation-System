import { useCallback, useEffect, useState } from 'react';
import { api } from './api.js';
import { errorText } from './components.jsx';

/** Active tables + all reservations of one restaurant on one day (staff/admin endpoints). */
export function useDayData(restaurantId, date) {
  const [tables, setTables] = useState([]);
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const reload = useCallback(() => {
    if (!restaurantId) return Promise.resolve();
    return Promise.all([
      api(`/api/restaurants/${restaurantId}/tables`),
      api('/api/reservations', { params: { restaurantId, date, size: 100 } }),
    ])
      .then(([t, r]) => {
        setTables(t.filter((x) => x.active));
        setReservations(r.content);
        setError('');
      })
      .catch((e) => setError(errorText(e)))
      .finally(() => setLoading(false));
  }, [restaurantId, date]);

  useEffect(() => {
    setLoading(true);
    reload();
  }, [reload]);

  return { tables, reservations, loading, error, reload };
}
