import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useTheme } from "../theme/ThemeProvider";
import { resolveMediaUrl } from "../lib/api";
import NotificationBell from "./NotificationBell";

const navigation = [
  { to: "/dashboard", label: "Home", icon: "⌂", end: true },
  { to: "/discover", label: "Discover", icon: "⌕" },
  { to: "/connections", label: "Network", icon: "◌" },
  { to: "/collabs", label: "Collabs", icon: "✦" },
  { to: "/chat", label: "Inbox", icon: "↗" },
  { to: "/highlights", label: "Highlights", icon: "◇" },
  { to: "/profile", label: "Profile", icon: "○" },
];

export default function AppShell() {
  const { student, logout } = useAuth();
  const { mode, toggle } = useTheme();
  const navigate = useNavigate();
  const initials = (student?.name || "S").charAt(0).toUpperCase();

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
            </NavLink>
          ))}
        </nav>
        <div className="app-sidebar__footer">
          <button type="button" className="app-profile-link" onClick={() => navigate("/profile")}>
            <Avatar student={student} initials={initials} />
            <span><strong>{student?.name || "My profile"}</strong><small>VIT-AP student</small></span>
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
            <button type="button" className="app-topbar__avatar" onClick={() => navigate("/profile")}>
              <Avatar student={student} initials={initials} />
            </button>
          </div>
        </header>
        <div className="app-mobile-nav">
          {navigation.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => isActive ? "is-active" : ""}>
              <span aria-hidden="true">{item.icon}</span>{item.label}
            </NavLink>
          ))}
        </div>
        <div className="app-content"><Outlet /></div>
      </main>
    </div>
  );
}

function NavBrand() {
  return (
    <div className="app-brand" aria-label="Connect">
      <span className="app-brand__mark" aria-hidden="true"><i /><i /><i /><i /></span>
      <span>CONNECT</span>
    </div>
  );
}

function Avatar({ student, initials }) {
  return student?.pictureUrl ? (
    <img className="app-avatar" src={resolveMediaUrl(student.pictureUrl)} alt="" referrerPolicy="no-referrer" />
  ) : (
    <span className="app-avatar app-avatar--fallback">{initials}</span>
  );
}
