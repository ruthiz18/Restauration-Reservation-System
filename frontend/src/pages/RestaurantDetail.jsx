import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, hhmm } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Alert, EmptyState, Spinner, errorText } from '../components.jsx';
import Icon from '../icons.jsx';
import { themeStyle } from '../themes.js';

export default function RestaurantDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const [restaurant, setRestaurant] = useState(null);
  const [menu, setMenu] = useState(null);
  const [rating, setRating] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [error, setError] = useState('');

  const loadReviews = useCallback(() => {
    api(`/api/restaurants/${id}/rating`).then(setRating).catch(() => {});
    api(`/api/restaurants/${id}/reviews`, { params: { size: 20 } }).then((d) => setReviews(d.content)).catch(() => {});
  }, [id]);

  useEffect(() => {
    setRestaurant(null);
    setError('');
    api(`/api/restaurants/${id}`).then(setRestaurant).catch((e) => setError(errorText(e)));
    api(`/api/restaurants/${id}/menu`).then(setMenu).catch(() => setMenu({ sections: [] }));
    loadReviews();
  }, [id, loadReviews]);

  if (error) return <div className="container"><Alert kind="error">{error}</Alert></div>;
  if (!restaurant) return <div className="container"><Spinner /></div>;

  return (
    <>
      <section className="detail-hero" style={themeStyle(restaurant.cuisine)}>
        <div className="container">
          <Link to="/" className="back"><Icon name="left" size={16} /> All restaurants</Link>
          <h1>{restaurant.name}</h1>
          <p className="detail-meta">
            <em className="tag">{restaurant.cuisine}</em>
            {rating?.count > 0
              ? <span><Icon name="star" size={16} className="star-on" /> {rating.average} ({rating.count} reviews)</span>
              : <span>No reviews yet</span>}
          </p>
        </div>
      </section>

      <div className="container two-col">
        <div>
          <div className="card card-pad">
            <p>{restaurant.description}</p>
            <p className="meta"><Icon name="pin" size={16} /> {restaurant.address}</p>
            {restaurant.phone && <p className="meta"><Icon name="phone" size={16} /> {restaurant.phone}</p>}
            <p className="meta"><Icon name="clock" size={16} /> Open {hhmm(restaurant.openTime)} &ndash; {hhmm(restaurant.closeTime)}</p>
          </div>

          <h2 className="section-title">Menu</h2>
          {!menu?.sections?.length && <EmptyState icon="menu" title="Menu not published yet" />}
          {menu?.sections?.map((s) => (
            <div className="card card-pad gap" key={s.name}>
              <h3>{s.name}</h3>
              {s.items.map((it) => (
                <div className="menu-item" key={it.name}>
                  <div>
                    <strong>{it.name}</strong>{' '}
                    {it.vegetarian && <span className="pill">vegetarian</span>}
                    {it.tags.map((t) => <span className="pill" key={t}>{t}</span>)}
                    {it.description && <div className="muted">{it.description}</div>}
                  </div>
                  <b>{Number(it.price).toFixed(2)}</b>
                </div>
              ))}
            </div>
          ))}

          <h2 className="section-title">Reviews</h2>
          <ReviewForm restaurantId={id} onSaved={loadReviews} />
          {reviews.length === 0 && <EmptyState icon="star" title="No reviews yet">Guests can review after a completed reservation.</EmptyState>}
          {reviews.map((r) => (
            <div className="card card-pad gap" key={r.id}>
              <div className="stars">{'★'.repeat(r.rating)}<span className="off">{'★'.repeat(5 - r.rating)}</span></div>
              <strong>{r.userName}</strong>
              {r.comment && <p>{r.comment}</p>}
            </div>
          ))}
        </div>

        <aside className="side-card card card-pad">
          <h3>Reserve a table</h3>
          <p className="muted">Pick a date, time and party size. Takes under a minute.</p>
          {user?.role === 'CUSTOMER' && (
            <Link className="btn btn-primary btn-block" to={`/restaurants/${restaurant.id}/book`}>Book a table</Link>
          )}
          {!user && (
            <Link className="btn btn-primary btn-block" to="/login" state={{ from: `/restaurants/${restaurant.id}/book` }}>
              Sign in to book
            </Link>
          )}
          {user && user.role !== 'CUSTOMER' && <Alert kind="info">Only customer accounts can make reservations.</Alert>}
        </aside>
      </div>
    </>
  );
}

function ReviewForm({ restaurantId, onSaved }) {
  const { user } = useAuth();
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [msg, setMsg] = useState({ kind: '', text: '' });
  if (user?.role !== 'CUSTOMER') return null;

  const submit = async (e) => {
    e.preventDefault();
    try {
      await api(`/api/restaurants/${restaurantId}/reviews`, { method: 'POST', body: { rating: Number(rating), comment } });
      setComment('');
      setMsg({ kind: 'success', text: 'Thanks for your review!' });
      onSaved();
    } catch (err) {
      setMsg({ kind: 'error', text: errorText(err) });
    }
  };

  return (
    <form className="card card-pad gap" onSubmit={submit}>
      <strong>Leave a review</strong>
      <span className="muted">Available after a completed reservation.</span>
      <label className="field"><span>Rating</span>
        <select value={rating} onChange={(e) => setRating(e.target.value)}>
          {[5, 4, 3, 2, 1].map((n) => <option key={n} value={n}>{n} star{n > 1 ? 's' : ''}</option>)}
        </select>
      </label>
      <label className="field"><span>Comment</span>
        <textarea rows="2" maxLength="1000" value={comment} onChange={(e) => setComment(e.target.value)} />
      </label>
      <div><button className="btn btn-primary">Submit review</button></div>
      <Alert kind={msg.kind || 'info'}>{msg.text}</Alert>
    </form>
  );
}
