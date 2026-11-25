import { useEffect, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useTheme } from "../theme/ThemeProvider";
import { api, hasLocalMedia, resolveMediaUrl } from "../lib/api";
import NotificationBell from "./NotificationBell";

const navigation = [
  { to: "/dashboard", label: "Home", icon: "⌂", end: true },
  { to: "/discover", label: "Discover", icon: "⌕" },
  { to: "/connections", label: "Network", icon: "◎" },
  { to: "/collabs", label: "Collabs", icon: "✦" },
  { to: "/chat", label: "Chat", icon: "✉" },
  { to: "/blogs", label: "Blogs", icon: "▤" },
  { to: "/profile", label: "Profile", icon: "○" },
];

export default function AppShell() {
  const { student, logout } = useAuth();
  const { mode, toggle } = useTheme();
  const navigate = useNavigate();
  const initials = (student?.name || "S").charAt(0).toUpperCase();
  const displayName = stripRegistrationNumber(student?.name || "My profile");
  const [unreadMessages, setUnreadMessages] = useState(0);
  const [unreadCollabRequests, setUnreadCollabRequests] = useState(false);

  useEffect(() => {
    let active = true;
    const loadUnread = () => {
      api.get("/chat/unread")
        .then((count) => active && setUnreadMessages(Number(count) || 0))
        .catch(() => {});
    };
    loadUnread();
    const onChatActivity = () => loadUnread();
    window.addEventListener("connect:chat-unread", onChatActivity);
    return () => {
      active = false;
      window.removeEventListener("connect:chat-unread", onChatActivity);
    };
  }, []);

  useEffect(() => {
    const onCollabActivity = (event) => setUnreadCollabRequests(Boolean(event.detail?.unread));
    window.addEventListener("connect:collab-activity", onCollabActivity);
    return () => window.removeEventListener("connect:collab-activity", onCollabActivity);
  }, []);

  const handleLogout = async () => {
    await logout();
    navigate("/", { replace: true });
  };

  return (
    <div className="app-shell">
      <aside className="app-sidebar">
        <NavBrand />
        <nav className="app-nav" aria-label="Main navigation">
          {navigation.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `app-nav__item${isActive ? " app-nav__item--active" : ""}`}
            >
              <span aria-hidden="true">{item.icon}</span>
              {item.label}
              {item.to === "/chat" && unreadMessages > 0 && (
                <span className="app-nav__dot" title={`${unreadMessages} unread messages`} aria-label={`${unreadMessages} unread messages`} />
              )}
              {item.to === "/collabs" && unreadCollabRequests && <span className="app-nav__dot" title="New collab request" aria-label="New collab request" />}
            </NavLink>
          ))}
        </nav>
        <div className="app-sidebar__footer">
          <button type="button" className="app-profile-link" onClick={() => navigate("/profile")}>
            <Avatar student={student} initials={initials} size="md" />
            <span>
              <strong title={displayName}>{displayName}</strong>
              <small>VIT-AP student</small>
            </span>
          </button>
          <button type="button" className="app-logout" onClick={handleLogout}>Sign out</button>
        </div>
      </aside>

      <main className="app-main">
        <header className="app-topbar">
          <div className="app-topbar__brand"><NavBrand /></div>
          <div className="app-topbar__actions">
            <button
              className="app-theme-toggle"
              onClick={toggle}
              type="button"
              aria-label={`Switch to ${mode === "dark" ? "light" : "dark"} mode`}
            >
              <span aria-hidden="true">{mode === "dark" ? "☼" : "◐"}</span>
              {mode === "dark" ? "Light" : "Dark"}
            </button>
            <NotificationBell />
            <button type="button" className="app-topbar__avatar" aria-label="Your profile" onClick={() => navigate("/profile")}>
              <Avatar student={student} initials={initials} size="sm" />
            </button>
          </div>
        </header>
        <nav className="app-mobile-nav" aria-label="Mobile navigation">
          {navigation.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => isActive ? "is-active" : ""}>
              <span aria-hidden="true">{item.icon}</span>{item.label}
              {item.to === "/chat" && unreadMessages > 0 && <span className="app-mobile-nav__dot" aria-hidden="true" />}
              {item.to === "/collabs" && unreadCollabRequests && <span className="app-mobile-nav__dot" aria-hidden="true" />}
            </NavLink>
          ))}
        </nav>
        <div className="app-content"><Outlet /></div>
      </main>
    </div>
  );
}

/** Removes a trailing roll/registration number (e.g. "23MIC7142") from a name. */
function stripRegistrationNumber(name) {
  return name.replace(/\s+\d{2}[A-Z]{2,4}\d{3,}\s*$/i, "");
}

function NavBrand() {
  return (
    <div className="app-brand" aria-label="Connect">
      <span className="app-brand__mark" aria-hidden="true"><i /><i /><i /><i /></span>
      <span>CONNECT</span>
    </div>
  );
}

function Avatar({ student, initials, size }) {
  return hasLocalMedia(student?.pictureUrl) ? (
    <img className={`avatar avatar--${size}`} src={resolveMediaUrl(student.pictureUrl)} alt="" referrerPolicy="no-referrer" />
  ) : (
    <span className={`avatar avatar--${size}`}>{initials}</span>
  );
}
