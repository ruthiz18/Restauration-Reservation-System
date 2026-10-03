import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, hhmm, today } from '../api.js';
import { Alert, Spinner, StatusBadge, errorText } from '../components.jsx';
import Icon from '../icons.jsx';
import { fmtDate } from '../util.js';

const STEPS = ['Date & Guests', 'Time', 'Confirm'];

/** 3-step booking flow. The server assigns the best-fitting free table when the booking is confirmed. */
export default function BookingWizard() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [restaurant, setRestaurant] = useState(null);
  const [step, setStep] = useState(1);
  const [date, setDate] = useState(today());
  const [partySize, setPartySize] = useState(2);
  const [slots, setSlots] = useState([]);
  const [time, setTime] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(null);

  useEffect(() => {
    api(`/api/restaurants/${id}`).then(setRestaurant).catch((e) => setError(errorText(e)));
  }, [id]);

  useEffect(() => {
    if (step !== 2 || !date) return;
    api(`/api/restaurants/${id}/availability`, { params: { date, partySize } })
      .then((s) => {
        setSlots(s);
        setTime((t) => (s.some((x) => hhmm(x.time) === t && x.availableTables > 0) ? t : ''));
      })
      .catch((e) => setError(errorText(e)));
  }, [step, id, date, partySize]);

  const confirm = async () => {
    setBusy(true);
    setError('');
    try {
      const r = await api('/api/reservations', {
        method: 'POST',
        body: { restaurantId: Number(id), date, time, partySize: Number(partySize), specialRequests: notes || null },
      });
      setDone(r);
    } catch (err) {
      setError(errorText(err));
      if (err.status === 409) setStep(2); // slot just got taken: pick another
    } finally {
      setBusy(false);
    }
  };

  if (!restaurant) return <div className="container narrow">{error ? <Alert kind="error">{error}</Alert> : <Spinner />}</div>;

  if (done) {
    return (
      <div className="container narrow">
        <div className="card success">
          <span className="success-ring"><Icon name="check" size={40} /></span>
          <h1>Reservation Requested!</h1>
          <p className="muted">The restaurant will confirm your table shortly. We&rsquo;ve emailed you the details.</p>
          <dl className="detail-list">
            <div><dt>Restaurant</dt><dd>{done.restaurantName}</dd></div>
            <div><dt>Date &amp; time</dt><dd>{fmtDate(done.date)} &middot; {hhmm(done.startTime)} &ndash; {hhmm(done.endTime)}</dd></div>
            <div><dt>Table</dt><dd>{done.tableLabel}</dd></div>
            <div><dt>Guests</dt><dd>{done.partySize}</dd></div>
            <div><dt>Status</dt><dd><StatusBadge status={done.status} /></dd></div>
          </dl>
          <div className="btn-row">
            <Link className="btn btn-primary" to={`/reservations/${done.id}`}>View reservation</Link>
            <button className="btn btn-outline" onClick={() => navigate('/')}>Browse restaurants</button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container narrow">
      <h1>New Reservation</h1>
      <p className="muted">{restaurant.name} &middot; {restaurant.address}</p>

      <ol className="stepper">
        {STEPS.map((s, i) => (
          <li key={s} className={step === i + 1 ? 'active' : step > i + 1 ? 'done' : ''}>
            <span className="dot">{step > i + 1 ? <Icon name="check" size={16} /> : i + 1}</span>
            <span className="lbl">{s}</span>
          </li>
        ))}
      </ol>

      <div className="card card-pad">
        <Alert kind="error">{error}</Alert>

        {step === 1 && (
          <>
            <label className="field"><span>Date</span>
              <input type="date" min={today()} value={date} onChange={(e) => setDate(e.target.value)} />
            </label>
            <div className="field">
              <span>Number of guests</span>
              <div className="counter">
                <button type="button" className="icon-btn" aria-label="Fewer guests" disabled={partySize <= 1}
                  onClick={() => setPartySize(Number(partySize) - 1)}><Icon name="minus" size={18} /></button>
                <b>{partySize}</b>
                <button type="button" className="icon-btn" aria-label="More guests" disabled={partySize >= 30}
                  onClick={() => setPartySize(Number(partySize) + 1)}><Icon name="plus" size={18} /></button>
              </div>
            </div>
            <div className="btn-row end">
              <button className="btn btn-primary" disabled={!date} onClick={() => { setError(''); setStep(2); }}>
                Next <Icon name="arrow" size={16} />
              </button>
            </div>
          </>
        )}

        {step === 2 && (
          <>
            <p className="muted">Available times on <strong>{fmtDate(date)}</strong> for {partySize} guest{partySize > 1 ? 's' : ''}</p>
            {slots.length === 0 && <p className="muted">No times left on this date &mdash; try another day.</p>}
            <div className="slots">
              {slots.map((s) => {
                const t = hhmm(s.time);
                return (
                  <button type="button" key={t} disabled={s.availableTables === 0}
                    className={`slot${time === t ? ' selected' : ''}`} onClick={() => setTime(t)}
                    title={`${s.availableTables} table(s) free`}>
                    <Icon name="clock" size={15} /> {t}
                  </button>
                );
              })}
            </div>
            <div className="btn-row between">
              <button className="btn btn-outline" onClick={() => setStep(1)}><Icon name="left" size={16} /> Back</button>
              <button className="btn btn-primary" disabled={!time} onClick={() => { setError(''); setStep(3); }}>
                Next <Icon name="arrow" size={16} />
              </button>
            </div>
          </>
        )}

        {step === 3 && (
          <>
            <div className="summary-bar">
              <span><Icon name="calendar" size={16} /> {fmtDate(date, { month: 'short', day: 'numeric', year: 'numeric' })}</span>
              <span><Icon name="clock" size={16} /> {time}</span>
              <span><Icon name="users" size={16} /> {partySize} guests</span>
            </div>
            <label className="field"><span>Special requests (optional)</span>
              <textarea rows="3" maxLength="500" placeholder="Allergies, celebrations, seating preferences…"
                value={notes} onChange={(e) => setNotes(e.target.value)} />
            </label>
            <div className="btn-row between">
              <button className="btn btn-outline" onClick={() => setStep(2)}><Icon name="left" size={16} /> Back</button>
              <button className="btn btn-primary" disabled={busy} onClick={confirm}>{busy ? 'Booking…' : 'Confirm Reservation'}</button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
