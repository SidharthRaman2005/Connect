import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

/**
 * First-time landing for newly created accounts. Profile onboarding (branch,
 * year, bio, skills, ...) is built in the Phase 2 profile modules; for now this
 * is a placeholder that lets the student continue into the app.
 */
export default function Onboarding() {
  const { student } = useAuth();

  return (
    <div style={{ maxWidth: 560, margin: "0 auto", padding: "2rem" }}>
      <h1>Welcome to Connect, {student.name || "student"}!</h1>
      <p style={{ color: "#555", lineHeight: 1.6 }}>
        Your account is set up. Profile onboarding — where you'll add your
        branch, year, bio, skills, and interests — is coming in the next module.
      </p>
      <p style={{ marginTop: "1.5rem" }}>
        <Link to="/dashboard">Continue to dashboard →</Link>
      </p>
    </div>
  );
}
