import { useState } from "react";
import { GoogleLogin } from "@react-oauth/google";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useTheme } from "../theme/ThemeProvider";

/**
 * Landing / sign-in page. The Google credential is sent to the backend, which
 * verifies it and enforces the college-email rule server-side — the frontend
 * does not decide who may sign in.
 */
export default function Login() {
  const { login, isAuthenticated, loading, authError, clearAuthError } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const { mode, toggle } = useTheme();

  // Already signed in (or just signed in): go to the app.
  if (!loading && isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleSuccess = async (credentialResponse) => {
    setError("");
    clearAuthError();
    setSubmitting(true);
    try {
      const res = await login(credentialResponse.credential);
      const destination = location.state?.from?.pathname;
      if (res.newAccount || !res.student.profileCompleted) {
        navigate("/onboarding", { replace: true });
      } else {
        navigate(destination || "/dashboard", { replace: true });
      }
    } catch (e) {
      setError(e.message || "Sign-in failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="auth-page">
      <button className="theme-toggle" onClick={toggle} type="button">
        <span aria-hidden="true">{mode === "dark" ? "☼" : "◐"}</span>
        {mode === "dark" ? "Light" : "Dark"}
      </button>
      <section className="auth-card">
        <div className="auth-brand" aria-label="Connect">
          <span className="auth-brand__mark" aria-hidden="true"><i /><i /><i /><i /></span>
          <span>CONNECT</span>
        </div>
        <p className="auth-eyebrow">VIT-AP COMMUNITY</p>
        <h1>Your campus.<br />Your people.</h1>
        <p className="auth-subtitle">
          Meet the people who will build what comes next. Made for the VIT-AP community.
        </p>
        <div className="auth-action">
          {submitting ? (
            <p className="auth-status">Signing you in…</p>
          ) : (
            <GoogleLogin
              onSuccess={handleSuccess}
              onError={() => setError("Google sign-in was cancelled or failed.")}
              text="continue_with"
              theme={mode === "dark" ? "filled_black" : "outline"}
              shape="pill"
              width="100%"
            />
          )}
        </div>
        {(error || authError) && <p className="auth-error" role="alert">{error || authError}</p>}
        <p className="auth-hint"><span aria-hidden="true">●</span> Only verified @vitapstudent.ac.in accounts can join</p>
        <p className="auth-footer">Find your next teammate. <span>·</span> Connect, collaborate, create</p>
      </section>
    </main>
  );
}
