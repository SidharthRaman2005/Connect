import { createContext, useContext, useEffect, useState, useCallback } from "react";
import { api, setAuthToken } from "../lib/api";

const TOKEN_KEY = "connect_token";

const AuthContext = createContext(null);

/**
 * Holds the authenticated session: the current student and token-backed
 * login/logout. On mount it restores a saved session by validating the stored
 * JWT against GET /api/auth/me.
 */
function readToken() {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [student, setStudent] = useState(null);
  // Start in the loading state only if there is a token to validate.
  const [loading, setLoading] = useState(() => Boolean(readToken()));

  useEffect(() => {
    const token = readToken();
    if (!token) {
      return; // nothing to restore; loading already false
    }

    // Validate the stored token and load the current student.
    setAuthToken(token);
    let active = true;
    api
      .get("/auth/me")
      .then((me) => {
        if (active) setStudent(me);
      })
      .catch(() => {
        if (active) clearSession(); // token invalid/expired
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  function clearSession() {
    try {
      localStorage.removeItem(TOKEN_KEY);
    } catch {
      /* ignore storage errors */
    }
    setAuthToken(null);
    setStudent(null);
  }

  /** Exchange a Google credential for a session; returns the AuthResponse. */
  const login = useCallback(async (googleIdToken) => {
    const res = await api.post("/auth/google", { idToken: googleIdToken });
    try {
      localStorage.setItem(TOKEN_KEY, res.token);
    } catch {
      /* ignore storage errors */
    }
    setAuthToken(res.token);
    setStudent(res.student);
    return res;
  }, []);

  /** Re-fetch the current student (e.g. after completing the profile). */
  const refresh = useCallback(async () => {
    try {
      const me = await api.get("/auth/me");
      setStudent(me);
      return me;
    } catch {
      return null;
    }
  }, []);

  /** Clear the local session (and best-effort notify the backend). */
  const logout = useCallback(async () => {
    try {
      await api.post("/auth/logout");
    } catch {
      /* logout is best-effort; clear locally regardless */
    }
    clearSession();
  }, []);

  const value = {
    student,
    loading,
    isAuthenticated: Boolean(student),
    login,
    logout,
    refresh,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
