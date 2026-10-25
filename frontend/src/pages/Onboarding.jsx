import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

/**
 * First-time landing for newly created accounts. Sends the student to the
 * profile editor to fill in their branch, bio, visibility, and tags (M2.1–M2.3).
 */
export default function Onboarding() {
  const { student } = useAuth();

  return (
    <div style={{ maxWidth: 560, margin: "0 auto", padding: "2rem" }}>
      <h1>Welcome to Connect, {student.name || "student"}!</h1>
      <p style={{ color: "#555", lineHeight: 1.6 }}>
        Your account is set up. Let's complete your profile — add your branch, a
        short bio, your visibility preference, and the skills, interests, and
        clubs that describe you.
      </p>
      <p style={{ marginTop: "1.5rem" }}>
        <Link to="/profile">Complete your profile →</Link>
      </p>
    </div>
  );
}
