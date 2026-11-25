import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, hasLocalMedia, resolveMediaUrl } from "../lib/api";

const TYPE_OPTIONS = [
  { value: "PROJECT", label: "Project" }, { value: "HACKATHON", label: "Hackathon" },
  { value: "RESEARCH", label: "Research" }, { value: "EVENT", label: "Event" }, { value: "OTHER", label: "Other" },
];

export default function AddCollab() {
  const [mine, setMine] = useState([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState({ title: "", description: "", type: "PROJECT" });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    try {
      setMine((await api.get("/collabs/me")) || []);
    } catch (requestError) {
      setError(requestError.message || "Failed to load your collabs.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  const update = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));
  const create = async (event) => {
    event.preventDefault();
    if (!form.title.trim()) {
      setError("Title is required.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      await api.post("/collabs", { title: form.title.trim(), description: form.description, type: form.type });
      setForm({ title: "", description: "", type: "PROJECT" });
      await load();
    } catch (requestError) {
      setError(requestError.message || "Failed to publish.");
    } finally {
      setSaving(false);
    }
  };
  const accept = async (id) => {
    try { await api.post(`/collabs/requests/${id}/accept`); await load(); }
    catch (requestError) { setError(requestError.message || "Failed to accept request."); }
  };
  const close = async (id) => {
    try { await api.post(`/collabs/${id}/close`); await load(); }
    catch (requestError) { setError(requestError.message || "Failed to close opportunity."); }
  };

  return (
    <div className="add-collab-page">
      <div className="page-heading">
        <div>
          <p className="page-eyebrow">YOUR OPPORTUNITY</p>
          <h1>Start something.</h1>
          <p className="page-subtitle">Tell the Connect community what you want to build and who you need.</p>
        </div>
        <Link className="page-heading__link" to="/collabs">Browse collabs <span>→</span></Link>
      </div>
      <form className="feature-form" onSubmit={create}>
        <label>Title <b>*</b><input value={form.title} onChange={update("title")} placeholder="What are you looking for?" maxLength={150} /></label>
        <label>Description<textarea value={form.description} onChange={update("description")} placeholder="What are you building, and who would you like to join?" maxLength={2000} /></label>
        <label>Opportunity type<select value={form.type} onChange={update("type")}>{TYPE_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
        {error && <p className="page-error" role="alert">{error}</p>}
        <button className="btn btn--primary" type="submit" disabled={saving}>{saving ? "Publishing…" : "Publish opportunity"} <span>→</span></button>
      </form>
      <section className="manage-section">
        <div className="section-title-row"><h2>Your collabs</h2><span>{mine.length}</span></div>
        {loading ? <div className="dashboard-loading"><span className="session-loading__spinner" /> Loading…</div> : mine.length ? mine.map((post) => <ManagedCollab key={post.id} post={post} onAccept={accept} onClose={close} />) : <p className="connection-empty">You haven&apos;t published any opportunities yet.</p>}
      </section>
    </div>
  );
}

function ManagedCollab({ post, onAccept, onClose }) {
  const pending = (post.requests || []).filter((request) => request.status === "PENDING");
  const members = (post.requests || []).filter((request) => request.status === "ACCEPTED");
  return (
    <article className="managed-collab">
      <div className="managed-collab__heading"><h3>{post.title}</h3><span className={`status-pill status-pill--${post.status === "OPEN" ? "open" : "closed"}`}>{post.status === "OPEN" ? "Open" : "Closed"}</span>{post.status === "OPEN" && <button type="button" className="btn btn--sm btn--secondary" onClick={() => onClose(post.id)}>Close</button>}</div>
      <p className="managed-collab__summary">{pending.length} pending · {members.length} in</p>
      {pending.map((request) => <Applicant key={request.id} applicant={request.applicant}><button className="btn btn--sm btn--primary" type="button" onClick={() => onAccept(request.id)}>Accept</button></Applicant>)}
      {members.map((request) => <Applicant key={request.id} applicant={request.applicant}><span className="status-pill status-pill--open">✓ In</span></Applicant>)}
      {!pending.length && !members.length && <p className="connection-empty">No requests yet.</p>}
    </article>
  );
}

function Applicant({ applicant, children }) {
  return <div className="applicant-row">{hasLocalMedia(applicant.pictureUrl) ? <img src={resolveMediaUrl(applicant.pictureUrl)} alt="" /> : <span>{(applicant.name || "?").charAt(0)}</span>}<div><Link to={`/students/${applicant.id}`}>{applicant.name || "Student"}</Link><small>{[applicant.branch, applicant.batch && `Batch ${applicant.batch}`].filter(Boolean).join(" · ")}</small></div><div className="applicant-row__action">{children}</div></div>;
}
