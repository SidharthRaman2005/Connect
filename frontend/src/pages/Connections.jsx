import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, resolveMediaUrl } from "../lib/api";

/**
 * Connection management (M3.2): respond to incoming requests, see/cancel
 * outgoing ones, and view/remove accepted connections. The "Simulate incoming"
 * button is a dev helper that makes dummy students send you requests.
 */
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
    } catch (e) {
      setError(e.message || "Failed to load connections.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const t = setTimeout(reload, 0);
    return () => clearTimeout(t);
  }, [reload]);

  const act = async (fn) => {
    try {
      await fn();
      await reload();
    } catch (e) {
      setError(e.message || "Action failed.");
    }
  };

  const accept = (id) => act(() => api.post(`/connections/${id}/accept`));
  const reject = (id) => act(() => api.post(`/connections/${id}/reject`));
  const remove = (id) => act(() => api.del(`/connections/${id}`));
  const simulate = () => act(() => api.post("/dev/seed-incoming"));

  if (loading) return <div style={{ padding: "2rem", textAlign: "center" }}>Loading…</div>;

  return (
    <div style={styles.page}>
      <div style={styles.headerRow}>
        <h1 style={{ margin: 0 }}>Connections</h1>
        <Link to="/dashboard" style={styles.muted}>← Dashboard</Link>
      </div>

      {error && <p style={styles.error} role="alert">{error}</p>}

      <Section title={`Requests received (${incoming.length})`}>
        {incoming.length === 0 ? (
          <Empty>No incoming requests.</Empty>
        ) : (
          incoming.map((c) => (
            <Row key={c.id} other={c.other}>
              <button style={styles.primary} onClick={() => accept(c.id)}>Accept</button>
              <button style={styles.ghost} onClick={() => reject(c.id)}>Reject</button>
            </Row>
          ))
        )}
      </Section>

      <Section title={`Requests sent (${outgoing.length})`}>
        {outgoing.length === 0 ? (
          <Empty>No pending sent requests.</Empty>
        ) : (
          outgoing.map((c) => (
            <Row key={c.id} other={c.other}>
              <button style={styles.ghost} onClick={() => remove(c.id)}>Cancel</button>
            </Row>
          ))
        )}
      </Section>

      <Section title={`Your connections (${accepted.length})`}>
        {accepted.length === 0 ? (
          <Empty>No connections yet.</Empty>
        ) : (
          accepted.map((c) => (
            <Row key={c.id} other={c.other}>
              <button style={styles.ghost} onClick={() => remove(c.id)}>Remove</button>
            </Row>
          ))
        )}
      </Section>

      <div style={styles.devBox}>
        <strong style={{ fontSize: "0.85rem" }}>Dev tools</strong>
        <p style={{ margin: "0.35rem 0", color: "#777", fontSize: "0.85rem" }}>
          No second account to test with? Make a few dummy students send you a request.
        </p>
        <button style={styles.ghost} onClick={simulate}>Simulate incoming requests</button>
      </div>
    </div>
  );
}

function Section({ title, children }) {
  return (
    <section style={{ marginBottom: "1.75rem" }}>
      <h2 style={styles.sectionTitle}>{title}</h2>
      {children}
    </section>
  );
}

function Empty({ children }) {
  return <p style={{ color: "#999", margin: "0.4rem 0" }}>{children}</p>;
}

function Row({ other, children }) {
  return (
    <div style={styles.row}>
      {other.pictureUrl ? (
        <img
          src={resolveMediaUrl(other.pictureUrl)}
          alt=""
          width={40}
          height={40}
          style={{ borderRadius: "50%", objectFit: "cover" }}
          referrerPolicy="no-referrer"
        />
      ) : (
        <div style={styles.avatarEmpty}>{(other.name || "?").charAt(0)}</div>
      )}
      <div style={{ flex: 1, minWidth: 0 }}>
        <Link to={`/students/${other.id}`} style={styles.name}>{other.name || "Student"}</Link>
        <div style={styles.meta}>
          {[other.branch, other.batch && `Batch ${other.batch}`].filter(Boolean).join(" · ")}
        </div>
      </div>
      <div style={{ display: "flex", gap: "0.5rem" }}>{children}</div>
    </div>
  );
}

const styles = {
  page: { maxWidth: 680, margin: "0 auto", padding: "2rem" },
  headerRow: { display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: "1.5rem" },
  sectionTitle: { fontSize: "1.05rem", borderBottom: "1px solid #eee", paddingBottom: "0.4rem" },
  row: { display: "flex", alignItems: "center", gap: "0.75rem", padding: "0.6rem 0", borderBottom: "1px solid #f3f3f3" },
  avatarEmpty: {
    width: 40,
    height: 40,
    borderRadius: "50%",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    background: "#e8eefc",
    color: "#1a73e8",
    fontWeight: 700,
    textTransform: "uppercase",
  },
  name: { fontWeight: 600, color: "#1a1a1a", textDecoration: "none" },
  meta: { color: "#666", fontSize: "0.85rem" },
  primary: { padding: "0.4rem 0.9rem", border: "none", borderRadius: 8, background: "#1a73e8", color: "#fff", cursor: "pointer" },
  ghost: { padding: "0.4rem 0.9rem", border: "1px solid #ccc", borderRadius: 8, background: "#fff", cursor: "pointer" },
  devBox: { marginTop: "1rem", padding: "1rem", border: "1px dashed #ccc", borderRadius: 10, background: "#fafafa" },
  muted: { color: "#888", textDecoration: "none" },
  error: { color: "#c0271c" },
};
