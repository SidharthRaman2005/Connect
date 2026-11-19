import { useEffect } from "react";

/**
 * A small, on-brand confirmation dialog for genuinely destructive actions.
 * Unlike window.confirm it is keyboard accessible, theme-aware and non-blocking.
 */
export default function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  busy = false,
  onConfirm,
  onCancel,
}) {
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (event) => {
      if (event.key === "Escape" && !busy) onCancel?.();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open, busy, onCancel]);

  if (!open) return null;

  return (
    <div className="modal-backdrop" onMouseDown={() => !busy && onCancel?.()}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="modal__icon" aria-hidden="true">!</div>
        <h2 className="modal__title" id="confirm-title">{title}</h2>
        <p className="modal__description">{description}</p>
        <div className="modal__actions">
          <button type="button" className="btn btn--secondary" onClick={onCancel} disabled={busy}>{cancelLabel}</button>
          <button type="button" className="btn btn--danger" onClick={onConfirm} disabled={busy}>
            {busy ? "Working…" : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
