import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, resolveMediaUrl } from "../lib/api";

const TYPE_OPTIONS = [
  { value: "PROJECT", label: "Project" },
  { value: "HACKATHON", label: "Hackathon" },
  { value: "RESEARCH", label: "Research" },
  { value: "EVENT", label: "Event" },
  { value: "OTHER", label: "Other" },
];

/**
 * Publish a collab post and manage your own (M4.2): accept join requests and
 * close a post when the team is full.
 */
export default function AddCollab() {
  const [mine, setMine] = useState([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState({ title: "", description: "", type: "PROJECT" });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(() => {
    api
      .get("/collabs/me")
      .then((p) => setMine(p || []))
      .catch((e) => setError(e.message || "Failed to load your collabs."))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const t = setTimeout(load, 0);
    return () => clearTimeout(t);
  }, [load]);

  const set = (field) => (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const create = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.title.trim()) {
      setError("Title is required.");
      return;
    }
    setSaving(true);
    try {
      await api.post("/collabs", {
        title: form.title.trim(),
        description: form.description,
        type: form.type,
      });
      setForm({ title: "", description: "", type: "PROJECT" });
      load();
    } catch (err) {
      setError(err.message || "Failed to publish.");
    } finally {
      setSaving(false);
    }
  };

  const accept = async (requestId) => {
    try {
      await api.post(`/collabs/requests/${requestId}/accept`);
      load();
    } catch (err) {
      setError(err.message || "Failed to accept.");
    }
  };

  const close = async (postId) => {
    try {
      await api.post(`/collabs/${postId}/close`);
      load();
    } catch (err) {
      setError(err.message || "Failed to close.");
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.headerRow}>
        <h1 style={{ margin: 0 }}>Add a collab</h1>
        <div style={{ display: "flex", gap: "1rem" }}>
          <Link to="/collabs" style={styles.link}>Recent posts</Link>
          <Link to="/dashboard" style={styles.muted}>← Dashboard</Link>
        </div>
      </div>

      <form onSubmit={create} style={styles.card}>
        <input style={styles.input} value={form.title} onChange={set("title")} placeholder="What are you looking for? (title) *" maxLength={150} />
        <textarea
          style={{ ...styles.input, minHeight: 90, resize: "vertical" }}
          value={form.description}
          onChange={set("description")}
          placeholder="Describe the collab — what you're building, who you need"
          maxLength={2000}
        />
        <select style={styles.input} value={form.type} onChange={set("type")}>
          {TYPE_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
        {error && <p style={styles.error} role="alert">{error}</p>}
        <button type="submit" disabled={saving} style={styles.primary}>
          {saving ? "Publishing…" : "Publish collab"}
        </button>
      </form>

      <h2 style={{ fontSize: "1.1rem" }}>Your collabs ({mine.length})</h2>
      {loading ? (
        <p style={{ color: "#777" }}>Loading…</p>
      ) : mine.length === 0 ? (
        <p style={{ color: "#777" }}>You haven't published any collabs yet.</p>
      ) : (
        mine.map((p) => {
          const pending = (p.requests || []).filter((r) => r.status === "PENDING");
          const members = (p.requests || []).filter((r) => r.status === "ACCEPTED");
          return (
            <div key={p.id} style={styles.item}>
              <div style={styles.itemHead}>
                <strong>{p.title}</strong>
                <span style={{ ...styles.badge, ...(p.status === "OPEN" ? styles.open : styles.closed) }}>
                  {p.status === "OPEN" ? "Open" : "Closed"}
                </span>
                {p.status === "OPEN" && (
                  <button type="button" onClick={() => close(p.id)} style={styles.closeBtn}>Close</button>
                )}
              </div>

              <div style={styles.subhead}>Requests ({pending.length} pending · {members.length} in)</div>
              {pending.length === 0 && members.length === 0 ? (
                <p style={{ color: "#999", margin: "0.3rem 0" }}>No requests yet.</p>
              ) : (
                <>
                  {pending.map((r) => (
                    <Row key={r.id} applicant={r.applicant}>
                      <button type="button" onClick={() => accept(r.id)} style={styles.accept}>Accept</button>
                    </Row>
                  ))}
                  {members.map((r) => (
                    <Row key={r.id} applicant={r.applicant}>
                      <span style={styles.inBadge}>✓ In</span>
                    </Row>
                  ))}
                </>
              )}
            </div>
          );
        })
      )}
    </div>
  );
}

function Row({ applicant, children }) {
  return (
    <div style={styles.row}>
      {applicant.pictureUrl ? (
        <img src={resolveMediaUrl(applicant.pictureUrl)} alt="" width={34} height={34} style={{ borderRadius: "50%", objectFit: "cover" }} referrerPolicy="no-referrer" />
      ) : (
        <div style={styles.avatarEmpty}>{(applicant.name || "?").charAt(0)}</div>
      )}
      <div style={{ flex: 1, minWidth: 0 }}>
        <Link to={`/students/${applicant.id}`} style={styles.link}>{applicant.name || "Student"}</Link>
        <div style={{ color: "#777", fontSize: "0.8rem" }}>
          {[applicant.branch, applicant.batch && `Batch ${applicant.batch}`].filter(Boolean).join(" · ")}
        </div>
      </div>
      {children}
    </div>
  );
}

const styles = {
  page: { maxWidth: 680, margin: "0 auto", padding: "2rem" },
  headerRow: { display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: "1.25rem", gap: "1rem", flexWrap: "wrap" },
  card: { border: "1px solid #e2e2e2", borderRadius: 12, padding: "1rem", marginBottom: "1.5rem", display: "flex", flexDirection: "column", gap: "0.6rem" },
  input: { width: "100%", padding: "0.5rem 0.65rem", fontSize: "1rem", border: "1px solid #ccc", borderRadius: 8, boxSizing: "border-box" },
  primary: { padding: "0.55rem 1.2rem", border: "none", borderRadius: 8, background: "#1a73e8", color: "#fff", cursor: "pointer", alignSelf: "flex-start" },
  item: { border: "1px solid #eee", borderRadius: 12, padding: "1rem", marginBottom: "0.85rem" },
  itemHead: { display: "flex", alignItems: "center", gap: "0.6rem", flexWrap: "wrap" },
  badge: { padding: "0.15rem 0.55rem", borderRadius: 999, fontSize: "0.75rem", fontWeight: 600 },
  open: { background: "#e6f4ea", color: "#188038" },
  closed: { background: "#f1f3f4", color: "#777" },
  closeBtn: { marginLeft: "auto", padding: "0.3rem 0.8rem", border: "1px solid #ccc", borderRadius: 8, background: "#fff", cursor: "pointer", fontSize: "0.85rem" },
  subhead: { fontSize: "0.8rem", textTransform: "uppercase", color: "#888", margin: "0.75rem 0 0.4rem" },
  row: { display: "flex", alignItems: "center", gap: "0.6rem", padding: "0.5rem 0", borderTop: "1px solid #f3f3f3" },
  avatarEmpty: { width: 34, height: 34, borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", background: "#e8eefc", color: "#1a73e8", fontWeight: 700, textTransform: "uppercase" },
  accept: { padding: "0.3rem 0.8rem", border: "none", borderRadius: 8, background: "#1a73e8", color: "#fff", cursor: "pointer", fontSize: "0.85rem" },
  inBadge: { padding: "0.3rem 0.7rem", borderRadius: 8, background: "#e6f4ea", color: "#188038", fontSize: "0.82rem", fontWeight: 600 },
  link: { color: "#1a73e8", textDecoration: "none", fontWeight: 600 },
  muted: { color: "#888", textDecoration: "none" },
  error: { color: "#c0271c", margin: 0 },
};
