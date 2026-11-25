import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, hasLocalMedia, resolveMediaUrl } from "../lib/api";
import { formatDateDDMMYY } from "../lib/dateFormat";

export default function ManageCollabs() {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [listError, setListError] = useState("");
  const [actionError, setActionError] = useState("");
  const [busyId, setBusyId] = useState(null);

  const load = useCallback(async () => {
    setListError("");
    try {
      setPosts((await api.get("/collabs/me")) || []);
    } catch (error) {
      setListError(error.message || "Could not load your collabs.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  const accept = async (requestId, postId) => {
    setBusyId(requestId);
    setActionError("");
    try {
      const accepted = await api.post(`/collabs/requests/${requestId}/accept`);
      setPosts((current) => current.map((post) => {
        if (post.id !== postId) return post;
        const requests = (post.requests || []).map((request) => request.id === requestId ? accepted : request);
        const acceptedCount = requests.filter((request) => request.status === "ACCEPTED").length;
        return { ...post, requests, acceptedCount, status: acceptedCount >= post.requiredPeople ? "COMPLETED" : post.status };
      }));
    } catch (error) {
      setActionError(error.message || "Could not accept this request.");
      await load();
    } finally {
      setBusyId(null);
    }
  };

  const close = async (postId) => {
    setBusyId(postId);
    setActionError("");
    try {
      const closed = await api.post(`/collabs/${postId}/close`);
      setPosts((current) => current.map((post) => post.id === postId ? closed : post));
    } catch (error) {
      setActionError(error.message || "Could not close this collab.");
      await load();
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="manage-collabs-page">
      <div className="page-heading">
        <div>
          <p className="page-eyebrow">YOUR COLLABORATIONS</p>
          <h1>My collabs.</h1>
          <p className="page-subtitle">Review applicants, accept teammates, and manage your opportunities.</p>
        </div>
        <div className="page-heading__actions">
          <Link className="page-heading__link" to="/collabs">Browse collabs <span>→</span></Link>
          <Link className="btn btn--primary" to="/collabs/add">Post an opportunity <span>+</span></Link>
        </div>
      </div>
      {actionError && <p className="page-error" role="alert">{actionError}</p>}
      {listError && <p className="page-error" role="alert">{listError} <button type="button" className="inline-retry" onClick={load}>Retry</button></p>}
      {loading ? <div className="dashboard-loading"><span className="session-loading__spinner" /> Loading your collabs…</div>
        : posts.length ? <div className="manage-collabs-list">{posts.map((post) => <ManagedCollab key={post.id} post={post} busyId={busyId} onAccept={accept} onClose={close} />)}</div>
          : !listError && <div className="empty-state"><span className="empty-state__icon" aria-hidden="true">✦</span><h2>No collabs yet</h2><p>Post an opportunity and requests from students will appear here.</p><Link className="btn btn--primary" to="/collabs/add">Create an opportunity <span>→</span></Link></div>}
    </div>
  );
}

function ManagedCollab({ post, busyId, onAccept, onClose }) {
  const requests = post.requests || [];
  const pending = requests.filter((request) => request.status === "PENDING");
  const members = requests.filter((request) => request.status === "ACCEPTED");
  const statusLabel = { OPEN: "Open", COMPLETED: "Completed", EXPIRED: "Expired", CLOSED: "Closed" }[post.status] || post.status;
  return (
    <article className={`managed-collab${post.status === "COMPLETED" ? " managed-collab--completed" : ""}`}>
      <div className="managed-collab__heading">
        <h2>{post.title}</h2>
        <span className={`status-pill status-pill--${post.status.toLowerCase()}`}>{statusLabel}</span>
        {post.status === "OPEN" && <button type="button" className="btn btn--sm btn--secondary" onClick={() => onClose(post.id)} disabled={busyId === post.id}>{busyId === post.id ? "Closing…" : "Close"}</button>}
      </div>
      <p className="managed-collab__summary">{post.acceptedCount ?? members.length} / {post.requiredPeople} accepted · {pending.length} pending</p>
      {post.status === "OPEN" && post.expiresAt && <p className="managed-collab__deadline">Recruitment closes {formatDateDDMMYY(post.expiresAt)}</p>}
      {requests.length ? <div className="managed-collab__requests">
        {pending.map((request) => <Applicant key={request.id} applicant={request.applicant}><button className="btn btn--sm btn--primary" type="button" onClick={() => onAccept(request.id, post.id)} disabled={busyId === request.id || post.status !== "OPEN"}>{busyId === request.id ? "Accepting…" : "Accept"}</button></Applicant>)}
        {members.map((request) => <Applicant key={request.id} applicant={request.applicant}><span className="status-pill status-pill--open">✓ Accepted</span></Applicant>)}
      </div> : <p className="connection-empty">No requests yet.</p>}
    </article>
  );
}

function Applicant({ applicant, children }) {
  return <div className="applicant-row">{hasLocalMedia(applicant.pictureUrl) ? <img src={resolveMediaUrl(applicant.pictureUrl)} alt="" /> : <span>{(applicant.name || "?").charAt(0)}</span>}<div><Link to={`/students/${applicant.id}`}>{applicant.name || "Student"}</Link><small>{[applicant.branch, applicant.batch && `Batch ${applicant.batch}`].filter(Boolean).join(" · ")}</small></div><div className="applicant-row__action">{children}</div></div>;
}
