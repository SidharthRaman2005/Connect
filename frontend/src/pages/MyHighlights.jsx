import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";

const STATUS_OPTIONS = [
  { value: "COMPLETED", label: "Completed" },
  { value: "IN_PROGRESS", label: "In progress" },
  { value: "IDEA", label: "Idea" },
];

const EMPTY = { title: "", description: "", tags: "", link: "", status: "COMPLETED" };

/**
 * Highlights manager (M4.1): list, add, edit, and delete the student's own
 * highlights — projects, achievements, sports, culture, anything worth showing.
 */
export default function MyHighlights() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(EMPTY);
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(() => {
    api
      .get("/highlights/me")
      .then((h) => setItems(h || []))
      .catch((e) => setError(e.message || "Failed to load highlights."))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const t = setTimeout(load, 0);
    return () => clearTimeout(t);
  }, [load]);

  const set = (field) => (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const resetForm = () => {
    setForm(EMPTY);
    setEditingId(null);
  };

  const startEdit = (h) => {
    setEditingId(h.id);
    setForm({
      title: h.title || "",
      description: h.description || "",
      tags: (h.tags || []).join(", "),
      link: h.link || "",
      status: h.status || "COMPLETED",
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.title.trim()) {
      setError("Title is required.");
      return;
    }
    setSaving(true);
    const payload = {
      title: form.title.trim(),
      description: form.description,
      tags: form.tags.split(",").map((t) => t.trim()).filter(Boolean),
      link: form.link,
      status: form.status,
    };
    try {
      if (editingId) await api.put(`/highlights/${editingId}`, payload);
      else await api.post("/highlights", payload);
      resetForm();
      load();
    } catch (err) {
      setError(err.message || "Failed to save highlight.");
    } finally {
      setSaving(false);
    }
  };

  const remove = async (id) => {
    try {
      await api.del(`/highlights/${id}`);
      if (editingId === id) resetForm();
      load();
    } catch (err) {
      setError(err.message || "Failed to delete highlight.");
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.headerRow}>
        <h1 style={{ margin: 0 }}>My highlights</h1>
        <Link to="/dashboard" style={styles.muted}>← Dashboard</Link>
      </div>
      <p style={{ color: "#777", marginTop: 0 }}>
        Show off anything — a project, a hackathon win, a national sports title, a research paper, a performance.
      </p>

      <form onSubmit={submit} style={styles.card}>
        <h2 style={{ marginTop: 0, fontSize: "1.1rem" }}>{editingId ? "Edit highlight" : "Add a highlight"}</h2>
        <input style={styles.input} value={form.title} onChange={set("title")} placeholder="Title * (e.g. Runner-up, State Badminton Championship)" maxLength={150} />
        <textarea
          style={{ ...styles.input, minHeight: 80, resize: "vertical" }}
          value={form.description}
          onChange={set("description")}
          placeholder="Describe it — what you did, the result, your role"
          maxLength={2000}
        />
        <input style={styles.input} value={form.tags} onChange={set("tags")} placeholder="Tags, comma-separated (e.g. Badminton, Sports, National)" />
        <div style={styles.two}>
          <input style={styles.input} value={form.link} onChange={set("link")} placeholder="Link (optional) — repo, article, certificate" />
          <select style={styles.input} value={form.status} onChange={set("status")}>
            {STATUS_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </div>

        {error && <p style={styles.error} role="alert">{error}</p>}

        <div style={{ display: "flex", gap: "0.6rem", marginTop: "0.5rem" }}>
          <button type="submit" disabled={saving} style={styles.primary}>
            {saving ? "Saving…" : editingId ? "Save changes" : "Add highlight"}
          </button>
          {editingId && <button type="button" onClick={resetForm} style={styles.secondary}>Cancel</button>}
        </div>
      </form>

      <h2 style={{ fontSize: "1.1rem" }}>Your highlights ({items.length})</h2>
      {loading ? (
        <p style={{ color: "#777" }}>Loading…</p>
      ) : items.length === 0 ? (
        <p style={{ color: "#777" }}>Nothing yet. Add your first highlight above.</p>
      ) : (
        items.map((h) => (
          <div key={h.id} style={styles.item}>
            <div style={styles.itemHead}>
              <strong>{h.title}</strong>
              <StatusBadge status={h.status} />
            </div>
            {h.description && <p style={styles.desc}>{h.description}</p>}
            {h.tags?.length > 0 && (
              <div style={styles.tags}>{h.tags.map((t) => <span key={t} style={styles.tag}>{t}</span>)}</div>
            )}
            {h.link && <a href={h.link} target="_blank" rel="noreferrer" style={styles.link}>Link ↗</a>}
            <div style={styles.itemActions}>
              <button type="button" onClick={() => startEdit(h)} style={styles.secondary}>Edit</button>
              <button type="button" onClick={() => remove(h.id)} style={styles.danger}>Delete</button>
            </div>
          </div>
        ))
      )}
    </div>
  );
}

function StatusBadge({ status }) {
  const map = {
    IDEA: { label: "Idea", bg: "#eef", color: "#3b4cca" },
    IN_PROGRESS: { label: "In progress", bg: "#fff4e5", color: "#a56b00" },
    COMPLETED: { label: "Completed", bg: "#e6f4ea", color: "#188038" },
  };
  const s = map[status] || map.COMPLETED;
  return <span style={{ ...styles.badge, background: s.bg, color: s.color }}>{s.label}</span>;
}

const styles = {
  page: { maxWidth: 680, margin: "0 auto", padding: "2rem" },
  headerRow: { display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: "0.75rem" },
  card: { border: "1px solid #e2e2e2", borderRadius: 12, padding: "1rem", marginBottom: "1.5rem", display: "flex", flexDirection: "column", gap: "0.6rem" },
  input: { width: "100%", padding: "0.5rem 0.65rem", fontSize: "1rem", border: "1px solid #ccc", borderRadius: 8, boxSizing: "border-box" },
  two: { display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.6rem" },
  primary: { padding: "0.55rem 1.2rem", border: "none", borderRadius: 8, background: "#1a73e8", color: "#fff", cursor: "pointer" },
  secondary: { padding: "0.45rem 1rem", border: "1px solid #ccc", borderRadius: 8, background: "#fff", cursor: "pointer" },
  danger: { padding: "0.45rem 1rem", border: "1px solid #f0c0bd", borderRadius: 8, background: "#fff", color: "#c0271c", cursor: "pointer" },
  item: { border: "1px solid #eee", borderRadius: 12, padding: "1rem", marginBottom: "0.85rem" },
  itemHead: { display: "flex", alignItems: "center", gap: "0.6rem", flexWrap: "wrap" },
  badge: { padding: "0.15rem 0.55rem", borderRadius: 999, fontSize: "0.75rem", fontWeight: 600 },
  desc: { color: "#444", lineHeight: 1.5, margin: "0.6rem 0" },
  tags: { display: "flex", flexWrap: "wrap", gap: "0.35rem", margin: "0.5rem 0" },
  tag: { padding: "0.2rem 0.55rem", borderRadius: 999, background: "#f1f3f4", fontSize: "0.8rem", color: "#444" },
  link: { color: "#1a73e8", textDecoration: "none", fontSize: "0.9rem" },
  itemActions: { display: "flex", gap: "0.5rem", marginTop: "0.5rem" },
  muted: { color: "#888", textDecoration: "none" },
  error: { color: "#c0271c", margin: 0 },
};
