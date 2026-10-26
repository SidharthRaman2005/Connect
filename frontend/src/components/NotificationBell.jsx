import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { api, resolveMediaUrl } from "../lib/api";

const POLL_MS = 30000;

/**
 * Header bell for pending incoming connection requests and collab acceptances
 * ("you're in"). A dot/count appears when there's anything new; opening the
 * dropdown marks collab notifications as seen so they clear.
 */
export default function NotificationBell() {
  const [requests, setRequests] = useState([]); // incoming connection requests
  const [collabs, setCollabs] = useState([]); // accepted-collab notifications
  const [open, setOpen] = useState(false);
  const [busyId, setBusyId] = useState(null);
  const ref = useRef(null);

  const load = useCallback(() => {
    api.get("/connections/incoming").then((r) => setRequests(r || [])).catch(() => {});
    api.get("/collabs/notifications").then((c) => setCollabs(c || [])).catch(() => {});
  }, []);

  useEffect(() => {
    load();
    const t = setInterval(load, POLL_MS);
    return () => clearInterval(t);
  }, [load]);

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
    if (next) {
      load();
      // Mark collab "you're in" notifications as seen once viewed.
      api.post("/collabs/notifications/seen").catch(() => {});
    }
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

  const count = requests.length + collabs.length;

  return (
    <div ref={ref} style={styles.wrap}>
      <button type="button" onClick={toggle} aria-label={`Notifications${count ? ` (${count} new)` : ""}`} style={styles.bellBtn}>
        <span aria-hidden="true">🔔</span>
        {count > 0 && <span style={styles.dot}>{count > 9 ? "9+" : count}</span>}
      </button>

      {open && (
        <div style={styles.panel} role="menu">
          {collabs.length > 0 && (
            <>
              <div style={styles.head}>You're in</div>
              {collabs.map((c) => (
                <Link key={c.requestId} to="/collabs" onClick={() => setOpen(false)} style={styles.collabItem}>
                  <span>✅ Accepted into <strong>{c.postTitle}</strong></span>
                  <span style={styles.sub}>by {c.ownerName}</span>
                </Link>
              ))}
            </>
          )}

          <div style={styles.head}>Connection requests</div>
          {requests.length === 0 ? (
            <p style={styles.empty}>No pending requests.</p>
          ) : (
            requests.map((c) => (
              <div key={c.id} style={styles.item}>
                {c.other.pictureUrl ? (
                  <img src={resolveMediaUrl(c.other.pictureUrl)} alt="" width={34} height={34} style={styles.avatar} referrerPolicy="no-referrer" />
                ) : (
                  <div style={styles.avatarEmpty}>{(c.other.name || "?").charAt(0)}</div>
                )}
                <div style={{ flex: 1, minWidth: 0 }}>
                  <Link to={`/students/${c.other.id}`} style={styles.name} onClick={() => setOpen(false)}>
                    {c.other.name || "Student"}
                  </Link>
                  <div style={styles.meta}>
                    {[c.other.branch, c.other.batch && `Batch ${c.other.batch}`].filter(Boolean).join(" · ")}
                  </div>
                </div>
                <div style={styles.actions}>
                  <button type="button" onClick={() => respond(c.id, true)} disabled={busyId === c.id} style={styles.accept}>Accept</button>
                  <button type="button" onClick={() => respond(c.id, false)} disabled={busyId === c.id} style={styles.reject}>Reject</button>
                </div>
              </div>
            ))
          )}
          <Link to="/connections" style={styles.viewAll} onClick={() => setOpen(false)}>View all connections →</Link>
        </div>
      )}
    </div>
  );
}

const styles = {
  wrap: { position: "relative" },
  bellBtn: { position: "relative", border: "none", background: "transparent", cursor: "pointer", fontSize: "1.4rem", lineHeight: 1, padding: "0.25rem" },
  dot: { position: "absolute", top: 0, right: 0, minWidth: 16, height: 16, padding: "0 4px", borderRadius: 999, background: "#d93025", color: "#fff", fontSize: "0.7rem", lineHeight: "16px", textAlign: "center", boxSizing: "border-box" },
  panel: { position: "absolute", top: "2.4rem", right: 0, width: 330, maxHeight: 420, overflowY: "auto", background: "#fff", border: "1px solid #e2e2e2", borderRadius: 12, boxShadow: "0 8px 24px rgba(0,0,0,0.12)", padding: "0.5rem", zIndex: 20 },
  head: { fontWeight: 700, padding: "0.5rem 0.5rem 0.4rem", fontSize: "0.9rem", borderBottom: "1px solid #f0f0f0" },
  empty: { color: "#999", padding: "0.6rem 0.5rem", margin: 0 },
  collabItem: { display: "flex", flexDirection: "column", padding: "0.5rem", textDecoration: "none", color: "#1a1a1a", borderBottom: "1px solid #f5f5f5", fontSize: "0.9rem" },
  sub: { color: "#777", fontSize: "0.78rem", marginTop: "0.1rem" },
  item: { display: "flex", alignItems: "center", gap: "0.5rem", padding: "0.55rem 0.5rem", borderBottom: "1px solid #f5f5f5" },
  avatar: { borderRadius: "50%", objectFit: "cover", flexShrink: 0 },
  avatarEmpty: { width: 34, height: 34, borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", background: "#e8eefc", color: "#1a73e8", fontWeight: 700, textTransform: "uppercase", flexShrink: 0 },
  name: { fontWeight: 600, color: "#1a1a1a", textDecoration: "none", fontSize: "0.9rem" },
  meta: { color: "#777", fontSize: "0.78rem" },
  actions: { display: "flex", flexDirection: "column", gap: "0.3rem" },
  accept: { padding: "0.25rem 0.6rem", border: "none", borderRadius: 6, background: "#1a73e8", color: "#fff", cursor: "pointer", fontSize: "0.8rem" },
  reject: { padding: "0.25rem 0.6rem", border: "1px solid #ccc", borderRadius: 6, background: "#fff", cursor: "pointer", fontSize: "0.8rem" },
  viewAll: { display: "block", textAlign: "center", padding: "0.6rem 0.5rem 0.3rem", color: "#1a73e8", textDecoration: "none", fontSize: "0.85rem" },
};
