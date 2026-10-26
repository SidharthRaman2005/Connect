import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";

const TYPE_LABELS = {
  PROJECT: "Project",
  HACKATHON: "Hackathon",
  RESEARCH: "Research",
  EVENT: "Event",
  OTHER: "Other",
};

/**
 * Recent collab posts feed (M4.2): browse open opportunities and send a request
 * to collaborate. The action reflects the viewer's relation to each post.
 */
export default function Collabs() {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  const load = useCallback(() => {
    api
      .get("/collabs")
      .then((p) => setPosts(p || []))
      .catch((e) => setError(e.message || "Failed to load collab posts."))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const t = setTimeout(load, 0);
    return () => clearTimeout(t);
  }, [load]);

  const request = async (id) => {
    setBusyId(id);
    setError("");
    try {
      await api.post(`/collabs/${id}/requests`);
      setPosts((prev) => prev.map((p) => (p.id === id ? { ...p, relation: "REQUESTED" } : p)));
    } catch (e) {
      setError(e.message || "Could not send request.");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.headerRow}>
        <h1 style={{ margin: 0 }}>Recent collab posts</h1>
        <div style={{ display: "flex", gap: "1rem" }}>
          <Link to="/collabs/add" style={styles.link}>+ Add collab</Link>
          <Link to="/dashboard" style={styles.muted}>← Dashboard</Link>
        </div>
      </div>

      {error && <p style={styles.error} role="alert">{error}</p>}

      {loading ? (
        <p style={{ color: "#777" }}>Loading…</p>
      ) : posts.length === 0 ? (
        <p style={{ color: "#777" }}>
          No open collab posts right now. <Link to="/collabs/add">Start one →</Link>
        </p>
      ) : (
        posts.map((p) => (
          <div key={p.id} style={styles.card}>
            <div style={styles.head}>
              <span style={styles.typeBadge}>{TYPE_LABELS[p.type] || p.type}</span>
              <strong style={{ fontSize: "1.05rem" }}>{p.title}</strong>
            </div>
            <div style={styles.by}>
              by <Link to={`/students/${p.owner.id}`} style={styles.link}>{p.owner.name}</Link>
              {p.owner.branch ? ` · ${p.owner.branch}` : ""}
            </div>
            {p.description && <p style={styles.desc}>{p.description}</p>}
            <Action post={p} onRequest={request} busy={busyId === p.id} />
          </div>
        ))
      )}
    </div>
  );
}

function Action({ post, onRequest, busy }) {
  switch (post.relation) {
    case "OWNER":
      return <Link to="/collabs/add" style={{ ...styles.btn, ...styles.btnMuted }}>Your post — manage</Link>;
    case "MEMBER":
      return <span style={{ ...styles.btn, ...styles.btnOk }}>✓ You're in</span>;
    case "REQUESTED":
      return <span style={{ ...styles.btn, ...styles.btnMuted }}>Requested</span>;
    default:
      return (
        <button type="button" onClick={() => onRequest(post.id)} disabled={busy} style={{ ...styles.btn, ...styles.btnDo }}>
          {busy ? "…" : "Request to collab"}
        </button>
      );
  }
}

const styles = {
  page: { maxWidth: 680, margin: "0 auto", padding: "2rem" },
  headerRow: { display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: "1.5rem", gap: "1rem", flexWrap: "wrap" },
  card: { border: "1px solid #e6e6e6", borderRadius: 12, padding: "1rem", marginBottom: "1rem", background: "#fff" },
  head: { display: "flex", alignItems: "center", gap: "0.6rem", flexWrap: "wrap" },
  typeBadge: { padding: "0.15rem 0.6rem", borderRadius: 999, fontSize: "0.72rem", fontWeight: 700, textTransform: "uppercase", background: "#e8eefc", color: "#1a73e8" },
  by: { color: "#666", fontSize: "0.85rem", marginTop: "0.35rem" },
  desc: { color: "#444", lineHeight: 1.5, margin: "0.6rem 0" },
  btn: { display: "inline-block", padding: "0.45rem 1rem", borderRadius: 8, fontSize: "0.9rem", fontWeight: 600, textDecoration: "none" },
  btnDo: { border: "none", background: "#1a73e8", color: "#fff", cursor: "pointer" },
  btnMuted: { background: "#f1f3f4", color: "#666" },
  btnOk: { background: "#e6f4ea", color: "#188038" },
  link: { color: "#1a73e8", textDecoration: "none", fontWeight: 600 },
  muted: { color: "#888", textDecoration: "none" },
  error: { color: "#c0271c" },
};
