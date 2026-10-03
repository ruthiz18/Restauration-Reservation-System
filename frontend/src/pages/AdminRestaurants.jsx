import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api, hhmm } from '../api.js';
import { Alert, PageHead, errorText } from '../components.jsx';
import Icon from '../icons.jsx';
import { useScope } from '../scope.jsx';

const empty = { name: '', description: '', cuisine: '', address: '', phone: '', openTime: '11:00', closeTime: '22:00' };

export default function AdminRestaurants() {
  const { restaurants, reload, setRestaurantId } = useScope();
  const [editing, setEditing] = useState(null); // null = closed, {id?, ...fields}
  const [msg, setMsg] = useState({ kind: '', text: '' });

  const save = async (e) => {
    e.preventDefault();
    const body = {
      name: editing.name, description: editing.description, cuisine: editing.cuisine, address: editing.address,
      phone: editing.phone, openTime: editing.openTime, closeTime: editing.closeTime,
    };
    try {
      if (editing.id) await api(`/api/restaurants/${editing.id}`, { method: 'PUT', body });
      else await api('/api/restaurants', { method: 'POST', body });
      setEditing(null);
      setMsg({ kind: 'success', text: 'Restaurant saved.' });
      reload();
    } catch (err) {
      setMsg({ kind: 'error', text: errorText(err) });
    }
  };

  const deactivate = async (r) => {
    if (!window.confirm(`Deactivate ${r.name}? It will disappear from public listings.`)) return;
    try {
      await api(`/api/restaurants/${r.id}`, { method: 'DELETE' });
      reload();
    } catch (err) {
      setMsg({ kind: 'error', text: errorText(err) });
    }
  };

  const set = (k) => (e) => setEditing({ ...editing, [k]: e.target.value });

  return (
    <>
      <PageHead title="Restaurants" sub="Create and manage the restaurants on the platform">
        <button className="btn btn-primary" onClick={() => setEditing({ ...empty })}><Icon name="plus" size={16} /> New restaurant</button>
      </PageHead>
      <Alert kind={msg.kind || 'info'}>{msg.text}</Alert>

      {editing && (
        <form className="card card-pad gap" onSubmit={save}>
          <h3>{editing.id ? 'Edit restaurant' : 'New restaurant'}</h3>
          <div className="grid-3">
            <label className="field"><span>Name</span><input required maxLength="120" value={editing.name} onChange={set('name')} /></label>
            <label className="field"><span>Cuisine</span><input required maxLength="60" value={editing.cuisine} onChange={set('cuisine')} /></label>
            <label className="field"><span>Phone</span><input maxLength="30" value={editing.phone || ''} onChange={set('phone')} /></label>
          </div>
          <label className="field"><span>Address</span><input required maxLength="200" value={editing.address} onChange={set('address')} /></label>
          <label className="field"><span>Description</span><textarea rows="2" maxLength="1000" value={editing.description || ''} onChange={set('description')} /></label>
          <div className="grid-2">
            <label className="field"><span>Opens</span><input type="time" required value={hhmm(editing.openTime)} onChange={set('openTime')} /></label>
            <label className="field"><span>Closes</span><input type="time" required value={hhmm(editing.closeTime)} onChange={set('closeTime')} /></label>
          </div>
          <div className="btn-row">
            <button className="btn btn-primary">Save</button>
            <button type="button" className="btn btn-outline" onClick={() => setEditing(null)}>Cancel</button>
          </div>
        </form>
      )}

      <div className="table-wrap card">
        <table className="tbl">
          <thead><tr><th>Name</th><th>Cuisine</th><th>Hours</th><th>Address</th><th></th></tr></thead>
          <tbody>
            {restaurants.map((r) => (
              <tr key={r.id}>
                <td><strong>{r.name}</strong></td><td>{r.cuisine}</td>
                <td>{hhmm(r.openTime)}&ndash;{hhmm(r.closeTime)}</td><td>{r.address}</td>
                <td>
                  <div className="btn-row tight">
                    <Link className="btn btn-outline btn-sm" to="/staff/tables" onClick={() => setRestaurantId(r.id)}>Tables</Link>
                    <button className="btn btn-outline btn-sm" onClick={() => setEditing({ ...r })}><Icon name="edit" size={15} /> Edit</button>
                    <button className="btn btn-danger-outline btn-sm" onClick={() => deactivate(r)}>Deactivate</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
