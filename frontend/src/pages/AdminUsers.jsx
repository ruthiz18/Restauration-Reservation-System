import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Alert, PageHead, Pager, Spinner, errorText } from '../components.jsx';
import { initials } from '../util.js';

const ROLES = ['CUSTOMER', 'STAFF', 'ADMIN'];
const SIZE = 20;

export default function AdminUsers() {
  const { user: me } = useAuth();
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    api('/api/users', { params: { page, size: SIZE } })
      .then((d) => { setData(d); setError(''); })
      .catch((e) => setError(errorText(e)));
  }, [page]);
  useEffect(load, [load]);

  const act = async (fn) => {
    try { await fn(); load(); } catch (e) { setError(errorText(e)); }
  };
  const setRole = (u, role) => act(() => api(`/api/users/${u.id}/role`, { method: 'PATCH', body: { role } }));
  const toggle = (u) => act(() => api(`/api/users/${u.id}/enabled`, { method: 'PATCH', body: { enabled: !u.enabled } }));

  return (
    <>
      <PageHead title="Users & roles" sub="Role-based access control: customers, staff and admins" />
      <Alert kind="error">{error}</Alert>
      {!data && !error && <Spinner />}
      {data && (
        <div className="table-wrap card">
          <table className="tbl">
            <thead><tr><th>User</th><th>Sign-in</th><th>Role</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {data.content.map((u) => (
                <tr key={u.id}>
                  <td>
                    <div className="user-cell">
                      <span className="avatar">{initials(u.fullName)}</span>
                      <span><strong>{u.fullName}</strong><br /><small className="muted">{u.email}</small></span>
                    </div>
                  </td>
                  <td>{u.provider}</td>
                  <td>
                    <select className="select-sm" value={u.role} disabled={u.id === me.id} onChange={(e) => setRole(u, e.target.value)}>
                      {ROLES.map((r) => <option key={r}>{r}</option>)}
                    </select>
                  </td>
                  <td><span className={`badge ${u.enabled ? 'b-COMPLETED' : 'b-CANCELLED'}`}>{u.enabled ? 'Active' : 'Disabled'}</span></td>
                  <td>
                    {u.id !== me.id && (
                      <button className={`btn btn-sm ${u.enabled ? 'btn-danger-outline' : 'btn-primary'}`} onClick={() => toggle(u)}>
                        {u.enabled ? 'Disable' : 'Enable'}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data && <Pager page={data.page} size={data.size} total={data.total} onChange={setPage} />}
    </>
  );
}
