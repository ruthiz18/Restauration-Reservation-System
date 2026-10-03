import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { Alert, EmptyState, PageHead, errorText } from '../components.jsx';
import Icon from '../icons.jsx';
import { useScope } from '../scope.jsx';

const blankItem = () => ({ name: '', description: '', price: 0, vegetarian: false, tags: '' });

// The menu is stored as a nested document in MongoDB; here it is edited as sections of items.
const toForm = (menu) =>
  (menu?.sections || []).map((s) => ({
    name: s.name,
    items: s.items.map((i) => ({ ...i, description: i.description || '', tags: (i.tags || []).join(', ') })),
  }));

const toPayload = (sections) =>
  sections.map((s) => ({
    name: s.name.trim(),
    items: s.items.map((i) => ({
      name: i.name.trim(),
      description: i.description.trim() || null,
      price: Number(i.price) || 0,
      vegetarian: !!i.vegetarian,
      tags: i.tags.split(',').map((t) => t.trim()).filter(Boolean),
    })),
  }));

export default function StaffMenu() {
  const { restaurantId, restaurant } = useScope();
  const [sections, setSections] = useState([]);
  const [msg, setMsg] = useState({ kind: '', text: '' });

  useEffect(() => {
    if (!restaurantId) return;
    api(`/api/restaurants/${restaurantId}/menu`)
      .then((m) => { setSections(toForm(m)); setMsg({ kind: '', text: '' }); })
      .catch((e) => setMsg({ kind: 'error', text: errorText(e) }));
  }, [restaurantId]);

  const updateSection = (si, patch) => setSections(sections.map((s, i) => (i === si ? { ...s, ...patch } : s)));
  const updateItem = (si, ii, patch) =>
    updateSection(si, { items: sections[si].items.map((it, i) => (i === ii ? { ...it, ...patch } : it)) });

  const save = async () => {
    try {
      const saved = await api(`/api/restaurants/${restaurantId}/menu`, { method: 'PUT', body: toPayload(sections) });
      setSections(toForm(saved));
      setMsg({ kind: 'success', text: 'Menu saved.' });
    } catch (e) {
      setMsg({ kind: 'error', text: errorText(e) });
    }
  };

  if (!restaurantId) return <EmptyState icon="store" title="No restaurant selected" />;

  return (
    <>
      <PageHead title="Menu" sub={restaurant ? `${restaurant.name} · stored as a document in MongoDB` : ''}>
        <button className="btn btn-outline" onClick={() => setSections([...sections, { name: '', items: [blankItem()] }])}>
          <Icon name="plus" size={16} /> Add section
        </button>
        <button className="btn btn-primary" onClick={save}>Save menu</button>
      </PageHead>
      <Alert kind={msg.kind || 'info'}>{msg.text}</Alert>

      {sections.length === 0 && <EmptyState icon="menu" title="This menu is empty">Add a section to get started.</EmptyState>}
      {sections.map((s, si) => (
        <div className="card card-pad gap" key={si}>
          <div className="card-head">
            <input aria-label="Section name" className="title-input" placeholder="Section (e.g. Starters)" value={s.name}
              onChange={(e) => updateSection(si, { name: e.target.value })} />
            <button className="btn btn-danger-outline btn-sm" onClick={() => setSections(sections.filter((_, i) => i !== si))}>
              <Icon name="trash" size={15} /> Remove section
            </button>
          </div>
          {s.items.map((it, ii) => (
            <div className="item-row" key={ii}>
              <label className="field"><span>Name</span><input value={it.name} onChange={(e) => updateItem(si, ii, { name: e.target.value })} /></label>
              <label className="field grow"><span>Description</span><input value={it.description} onChange={(e) => updateItem(si, ii, { description: e.target.value })} /></label>
              <label className="field narrow-f"><span>Price</span><input type="number" min="0" step="0.5" value={it.price} onChange={(e) => updateItem(si, ii, { price: e.target.value })} /></label>
              <label className="field"><span>Tags</span><input placeholder="vegan, spicy" value={it.tags} onChange={(e) => updateItem(si, ii, { tags: e.target.value })} /></label>
              <label className="check"><input type="checkbox" checked={it.vegetarian} onChange={(e) => updateItem(si, ii, { vegetarian: e.target.checked })} /> Vegetarian</label>
              <button className="icon-btn" aria-label="Remove item" onClick={() => updateSection(si, { items: s.items.filter((_, i) => i !== ii) })}>
                <Icon name="trash" size={18} />
              </button>
            </div>
          ))}
          <div><button className="btn btn-outline btn-sm" onClick={() => updateSection(si, { items: [...s.items, blankItem()] })}>
            <Icon name="plus" size={15} /> Add item
          </button></div>
        </div>
      ))}
    </>
  );
}
