import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, resolveMediaUrl } from "../lib/api";
import { useAuth } from "../auth/AuthContext";
import StudentCard from "../components/StudentCard";
import NotificationBell from "../components/NotificationBell";

/**
 * Home after sign-in. Shows the student's own header, navigation, and a
 * discovery feed of other students ranked by shared tags (M3.1) — so people
 * show up without searching. Connecting is done inline on each card (M3.2).
 */
export default function Dashboard() {
  const { student, logout } = useAuth();
  const navigate = useNavigate();

  const [feed, setFeed] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    let active = true;
    api
      .get("/discover")
      .then((list) => active && setFeed(list || []))
      .catch((e) => active && setError(e.message || "Failed to load suggestions."))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, []);

  const connect = useCallback(async (id) => {
    setBusyId(id);
    try {
      await api.post("/connections", { addresseeId: id });
      setFeed((prev) =>
        prev.map((c) => (c.id === id ? { ...c, relation: "PENDING_OUTGOING" } : c))
      );
    } catch (e) {
      setError(e.message || "Could not send request.");
    } finally {
      setBusyId(null);
    }
  }, []);

  const handleLogout = async () => {
    await logout();
    navigate("/", { replace: true });
  };

  return (
    <div style={styles.page}>
      <header style={styles.header}>
        <div style={styles.ident}>
          {student.pictureUrl && (
            <img
              src={resolveMediaUrl(student.pictureUrl)}
              alt=""
              width={44}
              height={44}
              style={{ borderRadius: "50%", objectFit: "cover" }}
              referrerPolicy="no-referrer"
            />
          )}
          <div>
            <strong>{student.name || "student"}</strong>
            <div style={{ color: "#777", fontSize: "0.85rem" }}>{student.email}</div>
          </div>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
          <NotificationBell />
          <button onClick={handleLogout} style={styles.logout}>Log out</button>
        </div>
      </header>

      <nav style={styles.nav}>
        <Link to="/profile" style={styles.navLink}>My profile</Link>
        <Link to="/search" style={styles.navLink}>Find people</Link>
        <Link to="/connections" style={styles.navLink}>Connections</Link>
      </nav>

      <h2 style={{ marginBottom: "0.25rem" }}>Suggested for you</h2>
      <p style={{ marginTop: 0, color: "#777" }}>Students who share your skills, interests, and clubs.</p>

      {error && <p style={styles.error} role="alert">{error}</p>}
      {loading ? (
        <p style={{ color: "#777" }}>Loading…</p>
      ) : feed.length === 0 ? (
        <p style={{ color: "#777" }}>
          No suggestions yet. Add tags on <Link to="/profile">your profile</Link> or{" "}
          <Link to="/search">search for people</Link>.
        </p>
      ) : (
        <div style={styles.grid}>
          {feed.map((card) => (
            <StudentCard key={card.id} card={card} onConnect={connect} busy={busyId === card.id} />
          ))}
        </div>
      )}
    </div>
  );
}

const styles = {
  page: { maxWidth: 760, margin: "0 auto", padding: "2rem" },
  header: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: "1.25rem",
  },
  ident: { display: "flex", alignItems: "center", gap: "0.75rem" },
  logout: { padding: "0.45rem 0.9rem", borderRadius: 8, border: "1px solid #ccc", background: "#fff", cursor: "pointer" },
  nav: {
    display: "flex",
    gap: "1rem",
    padding: "0.75rem 0",
    borderTop: "1px solid #eee",
    borderBottom: "1px solid #eee",
    marginBottom: "1.5rem",
  },
  navLink: { color: "#1a73e8", textDecoration: "none", fontWeight: 600 },
  grid: { display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))", gap: "1rem" },
  error: { color: "#c0271c" },
};
