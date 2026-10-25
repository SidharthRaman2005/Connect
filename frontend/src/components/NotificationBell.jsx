import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { api, resolveMediaUrl } from "../lib/api";

const POLL_MS = 30000;

/**
 * Header bell showing pending incoming connection requests. A dot/count appears
 * when there are any; the dropdown lets the student accept or reject each one
 * inline. Polls periodically so new requests surface without a page reload.
 */
export default function NotificationBell() {
  const [requests, setRequests] = useState([]);
  const [open, setOpen] = useState(false);
  const [busyId, setBusyId] = useState(null);
  const ref = useRef(null);

  const load = useCallback(() => {
    api.get("/connections/incoming").then((r) => setRequests(r || [])).catch(() => {});
  }, []);

  // Initial load + polling.
  useEffect(() => {
    load();
    const t = setInterval(load, POLL_MS);
    return () => clearInterval(t);
  }, [load]);

  // Close when clicking outside the bell/dropdown.
  useEffect(() => {
    if (!open) return undefined;
    const onClick = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, [open]);

  const toggle = () => {
    const next = !open;
    setOpen(next);
    if (next) load();
  };

  const respond = async (id, accept) => {
    setBusyId(id);
    try {
      await api.post(`/connections/${id}/${accept ? "accept" : "reject"}`);
      setRequests((prev) => prev.filter((c) => c.id !== id));
    } catch {
      load();
    } finally {
      setBusyId(null);
    }
  };

  const count = requests.length;

  return (
    <div ref={ref} style={styles.wrap}>
      <button
        type="button"
        onClick={toggle}
        aria-label={`Notifications${count ? ` (${count} new)` : ""}`}
        style={styles.bellBtn}
      >
        <span style={styles.bell} aria-hidden="true">🔔</span>
        {count > 0 && <span style={styles.dot}>{count > 9 ? "9+" : count}</span>}
      </button>

      {open && (
        <div style={styles.panel} role="menu">
          <div style={styles.panelHead}>Connection requests</div>
          {count === 0 ? (
            <p style={styles.empty}>No new requests.</p>
          ) : (
            requests.map((c) => (
              <div key={c.id} style={styles.item}>
                {c.other.pictureUrl ? (
                  <img
                    src={resolveMediaUrl(c.other.pictureUrl)}
                    alt=""
                    width={34}
                    height={34}
                    style={styles.avatar}
                    referrerPolicy="no-referrer"
                  />
                ) : (
                  <div style={styles.avatarEmpty}>{(c.other.name || "?").charAt(0)}</div>
                )}
                <div style={{ flex: 1, minWidth: 0 }}>
                  <Link to={`/students/${c.other.id}`} style={styles.name} onClick={() => setOpen(false)}>
                    {c.other.name || "Student"}
                  </Link>
                  <div style={styles.meta}>
                    {[c.other.branch, c.other.batch && `Batch ${c.other.batch}`]
                      .filter(Boolean)
                      .join(" · ")}
                  </div>
                </div>
                <div style={styles.actions}>
                  <button
                    type="button"
                    onClick={() => respond(c.id, true)}
                    disabled={busyId === c.id}
                    style={styles.accept}
                  >
                    Accept
                  </button>
                  <button
                    type="button"
                    onClick={() => respond(c.id, false)}
                    disabled={busyId === c.id}
                    style={styles.reject}
                  >
                    Reject
                  </button>
                </div>
              </div>
            ))
          )}
          <Link to="/connections" style={styles.viewAll} onClick={() => setOpen(false)}>
            View all connections →
          </Link>
        </div>
      )}
    </div>
  );
}

const styles = {
  wrap: { position: "relative" },
  bellBtn: {
    position: "relative",
    border: "none",
    background: "transparent",
    cursor: "pointer",
    fontSize: "1.4rem",
    lineHeight: 1,
    padding: "0.25rem",
  },
  bell: { filter: "grayscale(0.2)" },
  dot: {
    position: "absolute",
    top: 0,
    right: 0,
    minWidth: 16,
    height: 16,
    padding: "0 4px",
    borderRadius: 999,
    background: "#d93025",
    color: "#fff",
    fontSize: "0.7rem",
    lineHeight: "16px",
    textAlign: "center",
    boxSizing: "border-box",
  },
  panel: {
    position: "absolute",
    top: "2.4rem",
    right: 0,
    width: 320,
    maxHeight: 400,
    overflowY: "auto",
    background: "#fff",
    border: "1px solid #e2e2e2",
    borderRadius: 12,
    boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
    padding: "0.5rem",
    zIndex: 20,
  },
  panelHead: { fontWeight: 700, padding: "0.35rem 0.5rem 0.5rem", borderBottom: "1px solid #f0f0f0" },
  empty: { color: "#999", padding: "0.75rem 0.5rem", margin: 0 },
  item: { display: "flex", alignItems: "center", gap: "0.5rem", padding: "0.55rem 0.5rem", borderBottom: "1px solid #f5f5f5" },
  avatar: { borderRadius: "50%", objectFit: "cover", flexShrink: 0 },
  avatarEmpty: {
    width: 34,
    height: 34,
    borderRadius: "50%",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    background: "#e8eefc",
    color: "#1a73e8",
    fontWeight: 700,
    textTransform: "uppercase",
    flexShrink: 0,
  },
  name: { fontWeight: 600, color: "#1a1a1a", textDecoration: "none", fontSize: "0.9rem" },
  meta: { color: "#777", fontSize: "0.78rem" },
  actions: { display: "flex", flexDirection: "column", gap: "0.3rem" },
  accept: { padding: "0.25rem 0.6rem", border: "none", borderRadius: 6, background: "#1a73e8", color: "#fff", cursor: "pointer", fontSize: "0.8rem" },
  reject: { padding: "0.25rem 0.6rem", border: "1px solid #ccc", borderRadius: 6, background: "#fff", cursor: "pointer", fontSize: "0.8rem" },
  viewAll: { display: "block", textAlign: "center", padding: "0.6rem 0.5rem 0.3rem", color: "#1a73e8", textDecoration: "none", fontSize: "0.85rem" },
};
