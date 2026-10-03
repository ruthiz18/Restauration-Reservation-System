import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { Alert, EmptyState, PageHead, Pager, Spinner, errorText } from '../components.jsx';

const SIZE = 20;

/** Audit trail (MongoDB) of every email/SMS the RabbitMQ consumers dispatched. */
export default function AdminNotifications() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api('/api/notifications', { params: { page, size: SIZE } })
      .then((d) => { setData(d); setError(''); })
      .catch((e) => setError(errorText(e)));
  }, [page]);

  return (
    <>
      <PageHead title="Notification log" sub="Messages consumed from the RabbitMQ email and SMS queues" />
      <Alert kind="error">{error}</Alert>
      {!data && !error && <Spinner />}
      {data && data.content.length === 0 && <EmptyState icon="mail" title="Nothing yet">Make a reservation to generate events.</EmptyState>}
      {data && data.content.length > 0 && (
        <div className="table-wrap card">
          <table className="tbl">
            <thead><tr><th>When</th><th>Channel</th><th>Event</th><th>Reservation</th><th>Recipient</th><th>Status</th></tr></thead>
            <tbody>
              {data.content.map((n) => (
                <tr key={n.id}>
                  <td>{new Date(n.createdAt).toLocaleString()}</td>
                  <td><span className="pill">{n.channel}</span></td>
                  <td>{n.eventType}</td>
                  <td>#{n.reservationId}</td>
                  <td>{n.recipient || '—'}</td>
                  <td><span className={`badge ${n.status === 'SENT' ? 'b-COMPLETED' : n.status === 'FAILED' ? 'b-CANCELLED' : 'b-PENDING'}`}>{n.status}</span></td>
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
