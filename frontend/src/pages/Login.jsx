import { memo, useCallback, useState } from "react";
import { GoogleLogin } from "@react-oauth/google";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useTheme } from "../theme/ThemeProvider";

const HIGHLIGHTS = [
  { icon: "⌕", title: "Find your people", text: "Discover students who share your interests, skills and ambitions." },
  { icon: "✦", title: "Build together", text: "Post an opportunity or join a project, hackathon or research team." },
  { icon: "✉", title: "Stay connected", text: "Message your connections and keep every conversation in one place." },
];

/** Landing / sign-in page. The backend verifies the Google token and enforces the college-email rule. */
export default function Login() {
  const { login, isAuthenticated, loading, authError, clearAuthError } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const { mode, toggle } = useTheme();

  const handleSuccess = useCallback(async (credentialResponse) => {
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
    } catch (caught) {
      setError(caught.message || "Sign-in failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }, [clearAuthError, location.state, login, navigate]);

  const handleError = useCallback(() => {
    setError("Google sign-in was cancelled or failed.");
  }, []);

  if (!loading && isAuthenticated) return <Navigate to="/dashboard" replace />;

  return (
    <main className="auth-page">
      <button className="theme-toggle" onClick={toggle} type="button">
        <span aria-hidden="true">{mode === "dark" ? "☼" : "◐"}</span>
        {mode === "dark" ? "Light" : "Dark"}
      </button>
      <section className="auth-card">
        <div>
          <div className="auth-brand" aria-label="Connect">
            <span className="auth-brand__mark" aria-hidden="true"><i /><i /><i /><i /></span>
            <span>CONNECT</span>
          </div>
          <p className="auth-eyebrow">VIT-AP COMMUNITY</p>
          <h1>Your campus.<br />Your people.</h1>
          <p className="auth-subtitle">
            Meet the people who will build what comes next. One community, made for VIT-AP.
          </p>
        </div>
        <div className="auth-aside">
          <div className="auth-action">
            <p className="form-help">Sign in with your college Google account to continue.</p>
            {submitting ? (
              <p className="auth-status">Signing you in…</p>
            ) : (
              <GoogleSignInButton onSuccess={handleSuccess} onError={handleError} />
            )}
            {(error || authError) && <p className="auth-error" role="alert">{error || authError}</p>}
            <p className="auth-hint"><span aria-hidden="true">●</span> Only verified @vitapstudent.ac.in accounts can join</p>
          </div>
          <div className="auth-features">
            {HIGHLIGHTS.map((item) => (
              <div className="auth-aside__item" key={item.title}>
                <span className="auth-aside__icon" aria-hidden="true">{item.icon}</span>
                <div><strong>{item.title}</strong><span>{item.text}</span></div>
              </div>
            ))}
          </div>
        </div>
      </section>
    </main>
  );
}

const GoogleSignInButton = memo(function GoogleSignInButton({ onSuccess, onError }) {
  return <GoogleLogin onSuccess={onSuccess} onError={onError} text="continue_with" theme="filled_black" shape="pill" />;
});
