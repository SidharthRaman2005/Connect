import { useState } from "react";
import { GoogleLogin } from "@react-oauth/google";
import { Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

/**
 * Landing / sign-in page. The Google credential is sent to the backend, which
 * verifies it and enforces the college-email rule server-side — the frontend
 * does not decide who may sign in.
 */
export default function Login() {
  const { login, isAuthenticated, loading } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // Already signed in (or just signed in): go to the app.
  if (!loading && isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleSuccess = async (credentialResponse) => {
    setError("");
    setSubmitting(true);
    try {
      const res = await login(credentialResponse.credential);
      if (res.newAccount || !res.student.profileCompleted) {
        navigate("/onboarding", { replace: true });
      } else {
        navigate("/dashboard", { replace: true });
      }
    } catch (e) {
      setError(e.message || "Sign-in failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.card}>
        <h1 style={styles.title}>Connect</h1>
        <p style={styles.subtitle}>
          A networking space for VIT-AP students. Sign in with your college
          Google account to continue.
        </p>

        {submitting ? (
          <p>Signing you in…</p>
        ) : (
          <div style={styles.googleWrap}>
            <GoogleLogin
              onSuccess={handleSuccess}
              onError={() => setError("Google sign-in was cancelled or failed.")}
            />
          </div>
        )}

        {error && <p style={styles.error} role="alert">{error}</p>}

        <p style={styles.hint}>Only @vitapstudent.ac.in accounts are allowed.</p>
      </div>
    </div>
  );
}

const styles = {
  page: {
    minHeight: "100vh",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    padding: "1rem",
  },
  card: {
    maxWidth: 380,
    width: "100%",
    padding: "2rem",
    border: "1px solid #e2e2e2",
    borderRadius: 12,
    textAlign: "center",
  },
  title: { margin: "0 0 0.5rem", fontSize: "2rem" },
  subtitle: { margin: "0 0 1.5rem", color: "#555", lineHeight: 1.5 },
  googleWrap: { display: "flex", justifyContent: "center" },
  error: { color: "#c0271c", marginTop: "1rem" },
  hint: { marginTop: "1.5rem", fontSize: "0.85rem", color: "#888" },
};
