import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { api, resolveMediaUrl } from "../lib/api";

const POLL_MS = 30000;

/** Persisted event inbox for connection, collaboration, and message activity. */
export default function NotificationBell() {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState("");
  const ref = useRef(null);

  const load = useCallback(async () => {
    try {
      const inbox = await api.get("/notifications?page=0&size=30");
      setNotifications(inbox.content || []);
      setUnreadCount(inbox.unreadCount || 0);
      setError("");
    } catch (e) {
      setError(e.message || "Could not load notifications.");
    }
  }, []);

  useEffect(() => {
    const initial = setTimeout(load, 0);
    const poll = setInterval(load, POLL_MS);
    return () => { clearTimeout(initial); clearInterval(poll); };
  }, [load]);

  useEffect(() => {
    if (!open) return undefined;
    const onClick = (event) => {
      if (ref.current && !ref.current.contains(event.target)) setOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, [open]);

  const markRead = async (notification) => {
    if (notification.readAt) return;
    await api.post(`/notifications/${notification.id}/read`);
    setNotifications((items) => items.map((item) => item.id === notification.id
      ? { ...item, readAt: new Date().toISOString() } : item));
    setUnreadCount((count) => Math.max(0, count - 1));
  };

  const markAllRead = async () => {
    try {
      await api.post("/notifications/read-all");
      const readAt = new Date().toISOString();
      setNotifications((items) => items.map((item) => ({ ...item, readAt: item.readAt || readAt })));
      setUnreadCount(0);
    } catch (e) {
      setError(e.message || "Could not mark notifications as read.");
    }
  };

  const respond = async (notification, accept) => {
    setBusyId(notification.id);
    try {
      await api.post(`/connections/${notification.referenceId}/${accept ? "accept" : "reject"}`);
      await markRead(notification);
      await load();
    } catch (e) {
      setError(e.message || "Could not update the connection request.");
      await load();
    } finally {
      setBusyId(null);
    }
  };

  const toggle = () => {
    setOpen((value) => !value);
    if (!open) load();
  };

  return (
    <div ref={ref} style={styles.wrap}>
      <button type="button" onClick={toggle} aria-label={`Notifications${unreadCount ? ` (${unreadCount} unread)` : ""}`} style={styles.bellBtn}>
        <span aria-hidden="true">🔔</span>
        {unreadCount > 0 && <span style={styles.dot}>{unreadCount > 99 ? "99+" : unreadCount}</span>}
      </button>

      {open && (
        <div style={styles.panel} role="dialog" aria-label="Notifications">
          <div style={styles.panelHeader}>
            <strong>Notifications</strong>
            {unreadCount > 0 && <button type="button" onClick={markAllRead} style={styles.markAll}>Mark all read</button>}
          </div>
          {error && <p role="alert" style={styles.error}>{error}</p>}
          {notifications.length === 0 ? <p style={styles.empty}>No notifications yet.</p> : notifications.map((notification) => {
            const actor = notification.actor;
            const href = hrefFor(notification);
            const isConnectionRequest = notification.type === "CONNECTION_REQUEST";
            return (
              <div key={notification.id} style={{ ...styles.item, ...(notification.readAt ? {} : styles.unreadItem) }}>
                {actor?.pictureUrl ? <img src={resolveMediaUrl(actor.pictureUrl)} alt="" width={38} height={38} style={styles.avatar} referrerPolicy="no-referrer" />
                  : <div style={styles.avatarEmpty}>{(actor?.name || "•").charAt(0)}</div>}
                <div style={styles.itemBody}>
                  <Link to={href} onClick={() => { markRead(notification).catch(() => {}); setOpen(false); }} style={styles.message}>
                    {notification.message}
                  </Link>
                  <time style={styles.time} dateTime={notification.createdAt}>{new Date(notification.createdAt).toLocaleString([], { dateStyle: "short", timeStyle: "short" })}</time>
                  {isConnectionRequest && (
                    <div style={styles.actions}>
                      <button type="button" onClick={() => respond(notification, true)} disabled={busyId === notification.id} style={styles.accept}>Accept</button>
                      <button type="button" onClick={() => respond(notification, false)} disabled={busyId === notification.id} style={styles.reject}>Decline</button>
                    </div>
                  )}
                </div>
                {!notification.readAt && <span aria-label="Unread" style={styles.unreadDot} />}
              </div>
            );
          })}
          <button type="button" onClick={() => { setOpen(false); load(); }} style={styles.refresh}>Refresh</button>
        </div>
      )}
    </div>
  );
}

function hrefFor(notification) {
  switch (notification.type) {
    case "MESSAGE": return notification.actor ? `/chat?with=${notification.actor.id}` : "/chat";
    case "COLLAB_APPLICATION": return "/collabs/add";
    case "COLLAB_ACCEPTED": return "/collabs";
    default: return "/connections";
  }
}

const styles = {
  wrap: { position: "relative" },
  bellBtn: { position: "relative", border: "none", background: "transparent", cursor: "pointer", fontSize: "1.4rem", lineHeight: 1, padding: "0.25rem" },
  dot: { position: "absolute", top: 0, right: 0, minWidth: 16, height: 16, padding: "0 4px", borderRadius: 999, background: "#d93025", color: "#fff", fontSize: "0.7rem", lineHeight: "16px", textAlign: "center", boxSizing: "border-box" },
  panel: { position: "absolute", top: "2.4rem", right: 0, width: 350, maxHeight: 480, overflowY: "auto", background: "#fff", border: "1px solid #e2e2e2", borderRadius: 12, boxShadow: "0 8px 24px rgba(0,0,0,0.12)", padding: "0.5rem", zIndex: 20 },
  panelHeader: { display: "flex", justifyContent: "space-between", alignItems: "center", padding: ".45rem .5rem", borderBottom: "1px solid #eee" },
  markAll: { color: "#1a73e8", border: 0, background: "transparent", cursor: "pointer", fontSize: ".8rem" },
  empty: { color: "#999", padding: "0.6rem 0.5rem", margin: 0 },
  item: { position: "relative", display: "flex", alignItems: "flex-start", gap: ".55rem", padding: ".6rem .5rem", borderBottom: "1px solid #f5f5f5", borderRadius: 8 },
  unreadItem: { background: "#f5f8ff" },
  avatar: { borderRadius: "50%", objectFit: "cover", flexShrink: 0 },
  avatarEmpty: { width: 38, height: 38, borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", background: "#e8eefc", color: "#1a73e8", fontWeight: 700, textTransform: "uppercase", flexShrink: 0 },
  itemBody: { flex: 1, minWidth: 0 },
  message: { display: "block", color: "#222", textDecoration: "none", fontSize: ".87rem", lineHeight: 1.35 },
  time: { display: "block", color: "#888", fontSize: ".72rem", marginTop: ".2rem" },
  unreadDot: { width: 8, height: 8, borderRadius: "50%", background: "#1a73e8", marginTop: 6, flexShrink: 0 },
  actions: { display: "flex", gap: ".4rem", marginTop: ".4rem" },
  accept: { padding: ".25rem .6rem", border: 0, borderRadius: 6, background: "#1a73e8", color: "white", cursor: "pointer", fontSize: ".78rem" },
  reject: { padding: ".25rem .6rem", border: "1px solid #ccc", borderRadius: 6, background: "white", cursor: "pointer", fontSize: ".78rem" },
  error: { margin: ".4rem", color: "#c0271c", fontSize: ".82rem" },
  refresh: { display: "block", margin: ".4rem auto .1rem", padding: ".25rem .6rem", border: 0, background: "transparent", color: "#1a73e8", cursor: "pointer", fontSize: ".8rem" },
};
