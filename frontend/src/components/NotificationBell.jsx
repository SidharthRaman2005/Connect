import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { api, chatSocketUrl, hasLocalMedia, resolveMediaUrl } from "../lib/api";

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
      const items = inbox.content || [];
      setNotifications(items);
      dispatchCollabActivity(items);
      setUnreadCount(inbox.unreadCount || 0);
      setError("");
    } catch (e) {
      setError(e.message || "Could not load notifications.");
    }
  }, []);

  useEffect(() => {
    let stopped = false;
    let socket;
    let retryTimer;
    let attempts = 0;

    const reconnect = () => {
      if (stopped) return;
      const delay = Math.min(1000 * (2 ** attempts), 30000);
      attempts += 1;
      retryTimer = setTimeout(connect, delay);
    };

    const connect = async () => {
      try {
        const { ticket } = await api.post("/chat/socket-ticket");
        if (stopped) return;
        socket = new WebSocket(chatSocketUrl(ticket));
        socket.onopen = () => {
          attempts = 0;
          load();
          window.dispatchEvent(new Event("connect:chat-unread"));
        };
        socket.onmessage = (event) => {
          let message;
          try { message = JSON.parse(event.data); } catch { return; }
          if (message.type === "notification" || message.type === "notification_changed") load();
          if (["message", "read", "delivered"].includes(message.type)) {
            window.dispatchEvent(new Event("connect:chat-unread"));
          }
        };
        socket.onerror = () => socket.close();
        socket.onclose = reconnect;
      } catch {
        reconnect();
      }
    };

    const initialLoad = setTimeout(load, 0);
    connect();
    return () => {
      stopped = true;
      clearTimeout(initialLoad);
      clearTimeout(retryTimer);
      socket?.close();
    };
  }, [load]);

  useEffect(() => {
    if (!open) return undefined;
    const onClick = (event) => {
      if (ref.current && !ref.current.contains(event.target)) setOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, [open]);

  // Once a notification is seen/read it is deleted, so the inbox stays small.
  const markRead = async (notification) => {
    try {
      await api.del(`/notifications/${notification.id}`);
      setNotifications((items) => {
        const next = items.filter((item) => item.id !== notification.id);
        dispatchCollabActivity(next);
        return next;
      });
      if (!notification.readAt) setUnreadCount((count) => Math.max(0, count - 1));
    } catch (e) {
      setError(e.message || "Could not clear the notification.");
    }
  };

  const markAllRead = async () => {
    try {
      await api.del("/notifications/all");
      setNotifications([]);
      dispatchCollabActivity([]);
      setUnreadCount(0);
    } catch (e) {
      setError(e.message || "Could not clear notifications.");
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
    <div ref={ref} className="notification-bell">
      <button type="button" onClick={toggle} aria-label={`Notifications${unreadCount ? ` (${unreadCount} unread)` : ""}`} className="notification-bell__trigger">
        <span aria-hidden="true">🔔</span>
        {unreadCount > 0 && <span className="notification-bell__count">{unreadCount > 99 ? "99+" : unreadCount}</span>}
      </button>

      {open && (
        <div className="notification-panel" role="dialog" aria-label="Notifications">
          <div className="notification-panel__header">
            <strong>Notifications</strong>
            {unreadCount > 0 && <button type="button" onClick={markAllRead} className="notification-mark-all">Mark all read</button>}
          </div>
          {error && <p role="alert" className="notification-error">{error}</p>}
          {notifications.length === 0 ? <p className="notification-empty">No notifications yet.</p> : notifications.map((notification) => {
            const actor = notification.actor;
            const href = hrefFor(notification);
            const isConnectionRequest = notification.type === "CONNECTION_REQUEST";
            return (
              <div key={notification.id} className={`notification-item${notification.readAt ? "" : " is-unread"}`}>
                {hasLocalMedia(actor?.pictureUrl) ? <img src={resolveMediaUrl(actor.pictureUrl)} alt="" width={38} height={38} className="notification-avatar" />
                  : <div className="notification-avatar notification-avatar--empty">{(actor?.name || "•").charAt(0)}</div>}
                <div className="notification-item__body">
                  <Link to={href} onClick={() => { markRead(notification).catch(() => {}); setOpen(false); }} className="notification-message">
                    {notification.message}
                  </Link>
                  <time className="notification-time" dateTime={notification.createdAt}>{new Date(notification.createdAt).toLocaleString([], { dateStyle: "short", timeStyle: "short" })}</time>
                  {isConnectionRequest && (
                    <div className="notification-actions">
                      <button type="button" onClick={() => respond(notification, true)} disabled={busyId === notification.id} className="notification-accept">Accept</button>
                      <button type="button" onClick={() => respond(notification, false)} disabled={busyId === notification.id} className="notification-reject">Decline</button>
                    </div>
                  )}
                </div>
                {!notification.readAt && <span aria-label="Unread" className="notification-unread-dot" />}
              </div>
            );
          })}
          <button type="button" onClick={() => { setOpen(false); load(); }} className="notification-refresh">Refresh</button>
        </div>
      )}
    </div>
  );
}

function hrefFor(notification) {
  switch (notification.type) {
    case "MESSAGE": return notification.actor ? `/chat?with=${notification.actor.id}` : "/chat";
    case "COLLAB_APPLICATION": return "/collabs/manage";
    case "COLLAB_ACCEPTED": return "/collabs";
    default: return "/connections";
  }
}

function dispatchCollabActivity(notifications) {
  const unread = notifications.some((notification) => notification.type === "COLLAB_APPLICATION" && !notification.readAt);
  window.dispatchEvent(new CustomEvent("connect:collab-activity", { detail: { unread } }));
}

