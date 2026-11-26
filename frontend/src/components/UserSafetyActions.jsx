import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { useToast } from "./ToastProvider";
import ConfirmDialog from "./ConfirmDialog";

export default function UserSafetyActions({ studentId, showBlock = true, showReport = false }) {
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  const [blocked, setBlocked] = useState(false);
  const [blockConfirmOpen, setBlockConfirmOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [reportOpen, setReportOpen] = useState(false);

  useEffect(() => {
    if (!showBlock) return undefined;
    let active = true;
    api.get("/users/blocked")
      .then((items) => active && setBlocked((items || []).some((person) => person.id === studentId)))
      .catch(() => {});
    return () => { active = false; };
  }, [showBlock, studentId]);

  const confirmBlock = async () => {
    setBusy(true);
    try {
      await api.post(`/users/${studentId}/block`);
      setBlocked(true);
      setBlockConfirmOpen(false);
      toast.success("Messages from this student are now blocked.");
    } catch (error) {
      toast.error(error.message || "Could not block messages from this student.");
    } finally { setBusy(false); }
  };

  const unblock = async () => {
    setBusy(true);
    try {
      await api.del(`/users/${studentId}/block`);
      setBlocked(false);
      toast.success("Messages from this student are unblocked.");
    } catch (error) {
      toast.error(error.message || "Could not unblock this student.");
    } finally { setBusy(false); }
  };

  const report = async (event) => {
    event.preventDefault();
    setBusy(true);
    try {
      await api.post(`/users/${studentId}/reports`, { reason: reason.trim() });
      setReason("");
      setReportOpen(false);
      toast.success("Report submitted for review.");
    } catch (error) {
      toast.error(error.message || "Could not submit report.");
    } finally { setBusy(false); }
  };

  return <>
    <div className="user-safety-actions">
      {showBlock && (blocked ? (
        <button className="btn btn--sm btn--secondary" type="button" onClick={unblock} disabled={busy}>Unblock</button>
      ) : (
        <button className="btn btn--sm btn--secondary" type="button" onClick={() => setBlockConfirmOpen(true)} disabled={busy}>Block</button>
      ))}
      {showReport && <button className="btn btn--sm btn--ghost" type="button" onClick={() => setReportOpen((value) => !value)}>Report</button>}
      {showReport && reportOpen && <form onSubmit={report} className="user-safety-report">
        <label><span className="field__label">Why are you reporting this student?</span><textarea className="textarea" value={reason} onChange={(event) => setReason(event.target.value)} minLength={5} maxLength={1000} required /></label>
        <button className="btn btn--sm btn--primary" type="submit" disabled={busy || reason.trim().length < 5}>Submit report</button>
      </form>}
    </div>
    <ConfirmDialog
      open={blockConfirmOpen}
      title="Block messages from this student?"
      description="This only prevents their messages from reaching your chat. It does not remove your connection or change who can view your profile."
      confirmLabel="Block messages"
      busy={busy}
      onConfirm={confirmBlock}
      onCancel={() => setBlockConfirmOpen(false)}
    />
  </>;
}
