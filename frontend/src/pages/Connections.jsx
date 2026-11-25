import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, hasLocalMedia, resolveMediaUrl } from "../lib/api";

export default function Connections() {
  const [incoming, setIncoming] = useState([]);
  const [outgoing, setOutgoing] = useState([]);
  const [accepted, setAccepted] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    setError("");
    try {
      const [inc, out, acc] = await Promise.all([
        api.get("/connections/incoming"),
        api.get("/connections/outgoing"),
        api.get("/connections/accepted"),
      ]);
      setIncoming(inc || []);
      setOutgoing(out || []);
      setAccepted(acc || []);
    } catch (requestError) {
      setError(requestError.message || "Failed to load connections.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(reload, 0);
    return () => clearTimeout(timer);
  }, [reload]);

  const act = async (action) => {
    try {
      await action();
      await reload();
    } catch (requestError) {
      setError(requestError.message || "Action failed.");
    }
  };

  if (loading) return <div className="dashboard-loading"><span className="session-loading__spinner" /> Loading connections…</div>;

  return (
    <div className="connections-page">
      <div className="page-heading">
        <div>
          <p className="page-eyebrow">YOUR NETWORK</p>
          <h1>Connections.</h1>
          <p className="page-subtitle">Keep track of the people you want to build with.</p>
        </div>
        <Link className="page-heading__link" to="/discover">Find people <span>→</span></Link>
      </div>
      {error && <p className="page-error" role="alert">{error}</p>}
      <ConnectionSection title="Requests received" count={incoming.length}>
        {incoming.length ? incoming.map((connection) => (
          <ConnectionRow key={connection.id} other={connection.other}>
            <button className="btn btn--sm btn--primary" type="button" onClick={() => act(() => api.post(`/connections/${connection.id}/accept`))}>Accept</button>
            <button className="btn btn--sm btn--secondary" type="button" onClick={() => act(() => api.post(`/connections/${connection.id}/reject`))}>Decline</button>
          </ConnectionRow>
        )) : <EmptyState>No incoming requests right now.</EmptyState>}
      </ConnectionSection>
      <ConnectionSection title="Requests sent" count={outgoing.length}>
        {outgoing.length ? outgoing.map((connection) => (
          <ConnectionRow key={connection.id} other={connection.other}>
            <button className="btn btn--sm btn--secondary" type="button" onClick={() => act(() => api.del(`/connections/${connection.id}`))}>Cancel</button>
          </ConnectionRow>
        )) : <EmptyState>No pending requests.</EmptyState>}
      </ConnectionSection>
      <ConnectionSection title="Your connections" count={accepted.length}>
        {accepted.length ? accepted.map((connection) => (
          <ConnectionRow key={connection.id} other={connection.other}>
            <Link className="btn btn--sm btn--primary" to={`/chat?with=${connection.other.id}`}>Message</Link>
            <button className="btn btn--sm btn--secondary" type="button" onClick={() => act(() => api.del(`/connections/${connection.id}`))}>Remove</button>
          </ConnectionRow>
        )) : <EmptyState>No connections yet. Discover someone new to get started.</EmptyState>}
      </ConnectionSection>
    </div>
  );
}

function ConnectionSection({ title, count, children }) {
  return (
    <section className="connection-section">
      <div className="connection-section__heading"><h2>{title}</h2><span>{count}</span></div>
      <div className="connection-list">{children}</div>
    </section>
  );
}

function ConnectionRow({ other, children }) {
  return (
    <div className="connection-row">
      {hasLocalMedia(other.pictureUrl) ? <img src={resolveMediaUrl(other.pictureUrl)} alt="" /> : <span className="connection-row__avatar">{(other.name || "?").charAt(0)}</span>}
      <div className="connection-row__info">
        <Link to={`/students/${other.id}`}>{other.name || "Student"}</Link>
        <small>{[other.branch, other.batch && `Batch ${other.batch}`].filter(Boolean).join(" · ") || "VIT-AP student"}</small>
      </div>
      <div className="connection-row__actions">{children}</div>
    </div>
  );
}

function EmptyState({ children }) {
  return <p className="connection-empty">{children}</p>;
}
