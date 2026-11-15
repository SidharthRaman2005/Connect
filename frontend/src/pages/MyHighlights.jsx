import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";

const STATUS_OPTIONS = [{ value: "COMPLETED", label: "Completed" }, { value: "IN_PROGRESS", label: "In progress" }, { value: "IDEA", label: "Idea" }];
const EMPTY = { title: "", description: "", tags: "", link: "", status: "COMPLETED" };

export default function MyHighlights() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(EMPTY);
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    try { setItems((await api.get("/highlights/me")) || []); }
    catch (requestError) { setError(requestError.message || "Failed to load highlights."); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => {
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);
  const update = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));
  const reset = () => { setForm(EMPTY); setEditingId(null); };
  const edit = (item) => {
    setEditingId(item.id);
    setForm({ title: item.title || "", description: item.description || "", tags: (item.tags || []).join(", "), link: item.link || "", status: item.status || "COMPLETED" });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };
  const submit = async (event) => {
    event.preventDefault();
    if (!form.title.trim()) { setError("Title is required."); return; }
    setSaving(true);
    setError("");
    const payload = { title: form.title.trim(), description: form.description, tags: form.tags.split(",").map((tag) => tag.trim()).filter(Boolean), link: form.link, status: form.status };
    try {
      if (editingId) await api.put(`/highlights/${editingId}`, payload);
      else await api.post("/highlights", payload);
      reset();
      await load();
    } catch (requestError) { setError(requestError.message || "Failed to save highlight."); }
    finally { setSaving(false); }
  };
  const remove = async (id) => {
    try { await api.del(`/highlights/${id}`); if (editingId === id) reset(); await load(); }
    catch (requestError) { setError(requestError.message || "Failed to delete highlight."); }
  };

  return (
    <div className="highlights-page">
      <div className="page-heading">
        <div>
          <p className="page-eyebrow">YOUR WORK</p>
          <h1>Show your highlights.</h1>
          <p className="page-subtitle">Projects, wins, research, performances, and everything you&apos;re proud of.</p>
        </div>
        <Link className="page-heading__link" to="/profile">View profile <span>→</span></Link>
      </div>
      <form className="feature-form" onSubmit={submit}>
        <h2>{editingId ? "Edit highlight" : "Add a highlight"}</h2>
        <label>Title <b>*</b><input value={form.title} onChange={update("title")} placeholder="e.g. Runner-up, State Badminton Championship" maxLength={150} /></label>
        <label>Description<textarea value={form.description} onChange={update("description")} placeholder="What did you do, and what was your role?" maxLength={2000} /></label>
        <label>Tags<input value={form.tags} onChange={update("tags")} placeholder="Comma-separated: React, Sports, Research" /></label>
        <div className="feature-form__split"><label>Link <input value={form.link} onChange={update("link")} placeholder="Repository, article, or certificate" /></label><label>Status<select value={form.status} onChange={update("status")}>{STATUS_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label></div>
        {error && <p className="page-error" role="alert">{error}</p>}
        <div className="feature-form__actions"><button className="accent-button" type="submit" disabled={saving}>{saving ? "Saving…" : editingId ? "Save changes" : "Add highlight"} <span>→</span></button>{editingId && <button className="small-button" type="button" onClick={reset}>Cancel</button>}</div>
      </form>
      <section className="highlights-list">
        <div className="section-title-row"><h2>Your highlights</h2><span>{items.length}</span></div>
        {loading ? <div className="dashboard-loading"><span className="session-loading__spinner" /> Loading…</div> : items.length ? items.map((item) => <HighlightCard key={item.id} item={item} onEdit={edit} onRemove={remove} />) : <p className="connection-empty">Nothing here yet. Add your first highlight above.</p>}
      </section>
    </div>
  );
}

function HighlightCard({ item, onEdit, onRemove }) {
  return <article className="owned-highlight"><div className="owned-highlight__heading"><h3>{item.title}</h3><StatusBadge status={item.status} /></div>{item.description && <p>{item.description}</p>}{item.tags?.length > 0 && <div className="profile-tags">{item.tags.map((tag) => <span className="profile-tag" key={tag}>{tag}</span>)}</div>}{item.link && <a className="highlight-card__link" href={item.link} target="_blank" rel="noreferrer">Open link ↗</a>}<div className="owned-highlight__actions"><button className="small-button" type="button" onClick={() => onEdit(item)}>Edit</button><button className="small-button small-button--danger" type="button" onClick={() => onRemove(item.id)}>Delete</button></div></article>;
}

function StatusBadge({ status }) {
  const labels = { IDEA: "Idea", IN_PROGRESS: "In progress", COMPLETED: "Completed" };
  return <span className={`status-pill status-pill--${status.toLowerCase()}`}>{labels[status] || "Completed"}</span>;
}
