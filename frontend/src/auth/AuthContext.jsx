import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { api, setAuthToken, setUnauthorizedHandler } from "../lib/api";

const TOKEN_KEY = "connect_token";
const AuthContext = createContext(null);

function readToken() {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

function storeToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    // The in-memory session still works when browser storage is unavailable.
  }
}

export function AuthProvider({ children }) {
  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(() => Boolean(readToken()));
  const [authError, setAuthError] = useState(null);

  const clearSession = useCallback((message = null) => {
    storeToken(null);
    setAuthToken(null);
    setStudent(null);
    setAuthError(message);
    setLoading(false);
  }, []);

  const restoreSession = useCallback(async () => {
    const token = readToken();
    if (!token) {
      setAuthToken(null);
      setLoading(false);
      return;
    }

    setAuthToken(token);
    try {
      const me = await api.get("/auth/me");
      setStudent(me);
      setAuthError(null);
    } catch (error) {
      clearSession(
        error.status === 401
          ? "Your session expired. Please sign in again."
          : error.message || "Could not restore your session."
      );
    } finally {
      setLoading(false);
    }
  }, [clearSession]);

  useEffect(() => {
    setUnauthorizedHandler(() => clearSession("Your session expired. Please sign in again."));
    void Promise.resolve().then(restoreSession);

    const handleStorage = (event) => {
      if (event.key === TOKEN_KEY) void restoreSession();
    };
    window.addEventListener("storage", handleStorage);

    return () => {
      setUnauthorizedHandler(null);
      window.removeEventListener("storage", handleStorage);
    };
  }, [clearSession, restoreSession]);

  const login = useCallback(async (googleIdToken) => {
    if (!googleIdToken) {
      const error = new Error("Google sign-in did not return a credential.");
      setAuthError(error.message);
      throw error;
    }

    setAuthError(null);
    setLoading(true);
    try {
      const response = await api.post("/auth/google", { idToken: googleIdToken });
      if (!response?.token || !response.student) {
        throw new Error("The sign-in response was incomplete.");
      }
      storeToken(response.token);
      setAuthToken(response.token);
      setStudent(response.student);
      return response;
    } catch (error) {
      setAuthError(error.message || "Sign-in failed. Please try again.");
      throw error;
    } finally {
      setLoading(false);
    }
  }, []);

  const refresh = useCallback(async () => {
    try {
      const me = await api.get("/auth/me");
      setStudent(me);
      setAuthError(null);
      return me;
    } catch (error) {
      if (error.status === 401) clearSession("Your session expired. Please sign in again.");
      throw error;
    }
  }, [clearSession]);

  const logout = useCallback(async () => {
    try {
      await api.post("/auth/logout");
    } catch {
      // The token is cleared locally even if the stateless logout request fails.
    } finally {
      clearSession();
    }
  }, [clearSession]);

  const clearAuthError = useCallback(() => setAuthError(null), []);
  const value = useMemo(
    () => ({
      student,
      loading,
      authError,
      isAuthenticated: Boolean(student),
      login,
      logout,
      refresh,
      clearAuthError,
    }),
    [student, loading, authError, login, logout, refresh, clearAuthError]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}
