import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

/**
 * Minimal authenticated landing. Confirms the session works end to end and
 * provides logout. Real dashboard content arrives with later modules.
 */
export default function Dashboard() {
  const { student, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/", { replace: true });
  };

  return (
    <div style={{ maxWidth: 640, margin: "0 auto", padding: "2rem" }}>
      <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
        {student.pictureUrl && (
          <img
            src={student.pictureUrl}
            alt=""
            width={48}
            height={48}
            style={{ borderRadius: "50%" }}
            referrerPolicy="no-referrer"
          />
        )}
        <div>
          <h1 style={{ margin: 0 }}>Welcome, {student.name || "student"}</h1>
          <p style={{ margin: "0.25rem 0 0", color: "#555" }}>{student.email}</p>
        </div>
      </div>

      <p style={{ marginTop: "1.5rem", color: "#555" }}>
        You are signed in. Profiles, discovery, connections, and more are coming
        in the next modules.
      </p>

      <button onClick={handleLogout} style={{ marginTop: "1rem", padding: "0.5rem 1rem" }}>
        Log out
      </button>
    </div>
  );
}
