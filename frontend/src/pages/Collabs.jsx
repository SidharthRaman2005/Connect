import { useCallback, useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { api } from "../lib/api";
import { formatDateDDMMYY } from "../lib/dateFormat";

const TYPE_LABELS = { PROJECT: "Project", HACKATHON: "Hackathon", RESEARCH: "Research", EVENT: "Event", OTHER: "Other" };

export default function Collabs() {
  const location = useLocation();
  const createdPost = location.state?.createdPost;
  const [posts, setPosts] = useState(() => createdPost ? [createdPost] : []);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  const load = useCallback(async () => {
    setError("");
    try {
      const fetchedPosts = (await api.get("/collabs")) || [];
      setPosts(createdPost && !fetchedPosts.some((post) => post.id === createdPost.id)
        ? [createdPost, ...fetchedPosts]
        : fetchedPosts);
    } catch (requestError) {
      setError(requestError.message || "Failed to load collab opportunities.");
    } finally {
      setLoading(false);
    }
  }, [createdPost]);

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
        <div className="page-heading__actions">
          <Link className="btn btn--secondary" to="/collabs/manage">My collabs <span aria-hidden="true">→</span></Link>
          <Link className="btn btn--primary" to="/collabs/add">Post an opportunity <span aria-hidden="true">+</span></Link>
        </div>
      </div>
      {error && <p className="page-error" role="alert">{error}</p>}
      {loading ? (
        <div className="dashboard-loading"><span className="session-loading__spinner" /> Loading opportunities…</div>
      ) : error && !posts.length ? (
        <div className="empty-state">
          <h2>Couldn&apos;t load opportunities</h2>
          <p>The server didn&apos;t return the collab list. Try again in a moment.</p>
          <button className="btn btn--primary" type="button" onClick={load}>Try again <span aria-hidden="true">↻</span></button>
        </div>
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
  const completed = post.status === "COMPLETED";
  const expired = post.status === "EXPIRED";
  const open = post.status === "OPEN";
  const expiresAt = post.expiresAt ? new Date(post.expiresAt) : null;
  const terminalAt = post.terminalAt ? new Date(post.terminalAt) : null;
  const createdAt = post.createdAt ? new Date(post.createdAt) : null;
  const removeAt = terminalAt ? new Date(terminalAt.getTime() + 24 * 60 * 60 * 1000) : null;
  return (
    <article className={`collab-card${completed ? " collab-card--completed" : ""}${expired ? " collab-card--expired" : ""}`}>
      <div className="collab-card__main">
        <div className="collab-card__top">
          <span className="collab-type">{TYPE_LABELS[post.type] || post.type}</span>
          <span className={`collab-open${completed ? " collab-open--completed" : ""}${expired ? " collab-open--expired" : ""}`}>
            {completed ? "Collab completed" : expired ? "Collab expired" : "Open"}
          </span>
        </div>
        <h2>{post.title}</h2>
        <p className="collab-card__owner">by <Link to={`/students/${post.owner.id}`}>{post.owner.name}</Link>{post.owner.branch ? ` · ${post.owner.branch}` : ""}</p>
        {post.description && <p className="collab-card__description">{post.description}</p>}
      </div>
      <div className="collab-card__side">
        <div className="collab-card__capacity"><strong>{post.acceptedCount || 0} / {post.requiredPeople}</strong><span>teammates accepted</span></div>
        {open && expiresAt && <div className="collab-timeline">
          <div className="collab-timeline__label"><span>{createdAt ? `Posted ${formatDateDDMMYY(createdAt)}` : "Recruitment window"}</span><time dateTime={expiresAt.toISOString()}>Expires {formatDateDDMMYY(expiresAt)}</time></div>
          <div className="collab-timeline__track"><span /></div>
          <small>Fill the team within 5 days</small>
        </div>}
        {(completed || expired) && removeAt && <p className="collab-card__retention">This post will be removed on {formatDateDDMMYY(removeAt)}.</p>}
        <CollabAction post={post} busy={busy} onRequest={onRequest} />
      </div>
    </article>
  );
}

function CollabAction({ post, busy, onRequest }) {
  if (post.status === "COMPLETED") return <div className="collab-action collab-action--completed">Collab completed</div>;
  if (post.status === "EXPIRED") return <div className="collab-action collab-action--muted">Recruitment window expired</div>;
  if (post.relation === "OWNER") return <Link className="collab-action collab-action--muted" to="/collabs/add">Your post — manage <span>→</span></Link>;
  if (post.relation === "MEMBER") return <div className="collab-action collab-action--success">✓ You&apos;re in</div>;
  if (post.relation === "REQUESTED") return <div className="collab-action collab-action--muted">Request sent</div>;
  return <button type="button" className="collab-action" onClick={() => onRequest(post.id)} disabled={busy}>{busy ? "Sending…" : "Request to collaborate"} <span>→</span></button>;
}
