import { createContext, useCallback, useContext, useMemo, useState } from "react";

const ToastContext = createContext(null);
const TOAST_MS = 4200;

const ICONS = { success: "✓", error: "!", info: "i" };

/** Lightweight, non-blocking feedback. Replaces browser confirm/alert popups. */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const push = useCallback((message, tone = "success") => {
    if (!message) return;
    const id = `${Date.now()}-${Math.random()}`;
    setToasts((current) => [...current, { id, message, tone }]);
    setTimeout(() => dismiss(id), TOAST_MS);
  }, [dismiss]);

  const value = useMemo(() => ({
    success: (message) => push(message, "success"),
    error: (message) => push(message, "error"),
    info: (message) => push(message, "info"),
  }), [push]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-viewport" role="status" aria-live="polite">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast toast--${toast.tone}`}>
            <span className="toast__icon" aria-hidden="true">{ICONS[toast.tone]}</span>
            <span className="toast__message">{toast.message}</span>
            <button type="button" className="toast__close" aria-label="Dismiss" onClick={() => dismiss(toast.id)}>×</button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useToast() {
  const context = useContext(ToastContext);
  if (!context) throw new Error("useToast must be used within ToastProvider");
  return context;
}
