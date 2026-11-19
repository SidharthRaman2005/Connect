import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";

const TYPE_LABELS = { PROJECT: "Project", HACKATHON: "Hackathon", RESEARCH: "Research", EVENT: "Event", OTHER: "Other" };

export default function Collabs() {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  const load = useCallback(async () => {
    setError("");
    try {
      setPosts((await api.get("/collabs")) || []);
    } catch (requestError) {
      setError(requestError.message || "Failed to load collab opportunities.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  const request = async (id) => {
    setBusyId(id);
    try {
      await api.post(`/collabs/${id}/requests`);
      setPosts((current) => current.map((post) => post.id === id ? { ...post, relation: "REQUESTED" } : post));
    } catch (requestError) {
      setError(requestError.message || "Could not send collaboration request.");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="collabs-page">
      <div className="page-heading">
        <div>
          <p className="page-eyebrow">COLLABORATE</p>
          <h1>Build together.</h1>
          <p className="page-subtitle">Find open projects, hackathons, research teams, and campus opportunities.</p>
        </div>
        <Link className="btn btn--primary" to="/collabs/add">Post an opportunity <span aria-hidden="true">+</span></Link>
      </div>
      {error && <p className="page-error" role="alert">{error}</p>}
      {loading ? (
        <div className="dashboard-loading"><span className="session-loading__spinner" /> Loading opportunities…</div>
      ) : posts.length ? (
        <div className="collab-grid">
          {posts.map((post) => <CollabCard key={post.id} post={post} busy={busyId === post.id} onRequest={request} />)}
        </div>
      ) : (
        <div className="empty-state">
          <span className="empty-state__icon" aria-hidden="true">✦</span>
          <h2>No open opportunities yet</h2>
          <p>Start a project and invite the right people to build it with you.</p>
          <Link className="btn btn--primary" to="/collabs/add">Create the first post <span aria-hidden="true">→</span></Link>
        </div>
      )}
    </div>
  );
}

function CollabCard({ post, busy, onRequest }) {
  return (
    <article className="collab-card">
      <div className="collab-card__top">
        <span className="collab-type">{TYPE_LABELS[post.type] || post.type}</span>
        <span className="collab-open">Open</span>
      </div>
      <h2>{post.title}</h2>
      <p className="collab-card__owner">by <Link to={`/students/${post.owner.id}`}>{post.owner.name}</Link>{post.owner.branch ? ` · ${post.owner.branch}` : ""}</p>
      {post.description && <p className="collab-card__description">{post.description}</p>}
      <CollabAction post={post} busy={busy} onRequest={onRequest} />
    </article>
  );
}

function CollabAction({ post, busy, onRequest }) {
  if (post.relation === "OWNER") return <Link className="collab-action collab-action--muted" to="/collabs/add">Your post — manage <span>→</span></Link>;
  if (post.relation === "MEMBER") return <div className="collab-action collab-action--success">✓ You&apos;re in</div>;
  if (post.relation === "REQUESTED") return <div className="collab-action collab-action--muted">Request sent</div>;
  return <button type="button" className="collab-action" onClick={() => onRequest(post.id)} disabled={busy}>{busy ? "Sending…" : "Request to collaborate"} <span>→</span></button>;
}
