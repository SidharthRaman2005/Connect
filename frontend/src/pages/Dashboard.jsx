import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { resolveMediaUrl } from "../lib/api";
import { useAuth } from "../auth/AuthContext";

/**
 * Authenticated landing. Links to the profile editor (M2.1–M2.3) and provides a
 * quick way to open another student's public profile by id, for testing the
 * visibility-aware profile view.
 */
export default function Dashboard() {
  const { student, logout } = useAuth();
  const navigate = useNavigate();
  const [lookupId, setLookupId] = useState("");

  const handleLogout = async () => {
    await logout();
    navigate("/", { replace: true });
  };

  const handleLookup = (e) => {
    e.preventDefault();
    const id = lookupId.trim();
    if (id) navigate(`/students/${id}`);
  };

  return (
    <div style={{ maxWidth: 640, margin: "0 auto", padding: "2rem" }}>
      <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
        {student.pictureUrl && (
          <img
            src={resolveMediaUrl(student.pictureUrl)}
            alt=""
            width={48}
            height={48}
            style={{ borderRadius: "50%", objectFit: "cover" }}
            referrerPolicy="no-referrer"
          />
        )}
        <div>
          <h1 style={{ margin: 0 }}>Welcome, {student.name || "student"}</h1>
          <p style={{ margin: "0.25rem 0 0", color: "#555" }}>{student.email}</p>
          <p style={{ margin: "0.1rem 0 0", color: "#999", fontSize: "0.85rem" }}>
            Your student id: {student.id}
          </p>
        </div>
      </div>

      <p style={{ marginTop: "1.5rem" }}>
        <Link to="/profile">View &amp; edit your profile →</Link>
      </p>

      <form onSubmit={handleLookup} style={{ marginTop: "1.5rem" }}>
        <label style={{ display: "block", fontWeight: 600, marginBottom: "0.4rem" }}>
          Open a student's public profile{" "}
          <span style={{ fontWeight: 400, color: "#999" }}>
            (try your own id, {student.id})
          </span>
        </label>
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <input
            value={lookupId}
            onChange={(e) => setLookupId(e.target.value)}
            placeholder="Student id, e.g. 1"
            inputMode="numeric"
            style={{
              flex: 1,
              padding: "0.5rem 0.65rem",
              border: "1px solid #ccc",
              borderRadius: 8,
            }}
          />
          <button type="submit" style={{ padding: "0.5rem 1rem", borderRadius: 8 }}>
            View
          </button>
        </div>
      </form>

      <button onClick={handleLogout} style={{ marginTop: "2rem", padding: "0.5rem 1rem" }}>
        Log out
      </button>
    </div>
  );
}
