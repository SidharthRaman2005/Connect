import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useTheme } from "../theme/ThemeProvider";

/**
 * Guards routes that require a signed-in student. While the session is being
 * restored it shows a loading state; unauthenticated users are sent to login.
 */
export default function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();
  const { mode } = useTheme();
  const location = useLocation();

  if (loading) {
    return (
      <main className="session-loading" data-theme={mode}>
        <div className="session-loading__spinner" aria-hidden="true" />
        <p>Restoring your Connect session…</p>
      </main>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/" replace state={{ from: location }} />;
  }

  return children;
}
