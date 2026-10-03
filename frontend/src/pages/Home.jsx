import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, hhmm } from '../api.js';
import { Alert, EmptyState, Pager, Spinner, errorText } from '../components.jsx';
import Icon from '../icons.jsx';
import { themeStyle } from '../themes.js';

const SIZE = 9;

export default function Home() {
  const [q, setQ] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setData(null);
    api('/api/restaurants', { params: { q: query, page, size: SIZE } })
      .then((d) => { setData(d); setError(''); })
      .catch((e) => setError(errorText(e)));
  }, [query, page]);

  const submit = (e) => {
    e.preventDefault();
    setPage(0);
    setQuery(q.trim());
  };

  return (
    <>
      <section className="hero">
        <div className="hero-inner">
          <h1>Find your next table</h1>
          <p>Browse restaurants, read menus and reserve in seconds.</p>
          <form className="hero-search" onSubmit={submit}>
            <Icon name="search" />
            <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search by name or cuisine"
              aria-label="Search restaurants" />
            <button className="btn btn-primary">Search</button>
          </form>
        </div>
      </section>

      <div className="container">
        <Alert kind="error">{error}</Alert>
        {!data && !error && <Spinner />}
        {data && data.content.length === 0 && (
          <EmptyState icon="search" title="No restaurants found">Try a different name or cuisine.</EmptyState>
        )}
        <section className="r-grid">
          {data?.content.map((r) => (
            <Link to={`/restaurants/${r.id}`} className="r-card" key={r.id}>
              <div className="r-thumb" style={themeStyle(r.cuisine)}>
                <em className="tag">{r.cuisine}</em>
              </div>
              <div className="r-body">
                <h3>{r.name}</h3>
                <p className="clamp">{r.description}</p>
                <p className="meta"><Icon name="pin" size={15} /> {r.address}</p>
                <p className="meta"><Icon name="clock" size={15} /> {hhmm(r.openTime)} &ndash; {hhmm(r.closeTime)}</p>
                <span className="r-cta">View &amp; reserve <Icon name="arrow" size={16} /></span>
              </div>
            </Link>
          ))}
        </section>
        {data && <Pager page={data.page} size={data.size} total={data.total} onChange={setPage} />}
      </div>
    </>
  );
}
