import { useState } from "react";
import { api } from "../lib/api";
import { useToast } from "./ToastProvider";
import ConfirmDialog from "./ConfirmDialog";

export default function UserSafetyActions({ studentId, onBlocked, showReport = true }) {
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  const [reason, setReason] = useState("");
  const [reportOpen, setReportOpen] = useState(false);
  const [blockConfirmOpen, setBlockConfirmOpen] = useState(false);

  const block = async () => {
    setBusy(true);
    try {
      await api.post(`/users/${studentId}/block`);
      setBlockConfirmOpen(false);
      toast.success("Student blocked.");
      onBlocked?.();
    } catch (error) { toast.error(error.message || "Could not block this student."); }
    finally { setBusy(false); }
  };

  const report = async (event) => {
    event.preventDefault();
    setBusy(true);
    try {
      await api.post(`/users/${studentId}/reports`, { reason: reason.trim() });
      setReason("");
      setReportOpen(false);
      toast.success("Report submitted for review.");
    } catch (error) { toast.error(error.message || "Could not submit report."); }
    finally { setBusy(false); }
  };

  return <>
    <div className="user-safety-actions">
      <button className="btn btn--sm btn--secondary" type="button" onClick={() => setBlockConfirmOpen(true)} disabled={busy}>Block</button>
      {showReport && <>
        <button className="btn btn--sm btn--ghost" type="button" onClick={() => setReportOpen((value) => !value)}>Report</button>
        {reportOpen && <form onSubmit={report} className="user-safety-report">
          <label><span className="field__label">Why are you reporting this student?</span><textarea className="textarea" value={reason} onChange={(event) => setReason(event.target.value)} minLength={5} maxLength={1000} required /></label>
          <button className="btn btn--sm btn--primary" disabled={busy || reason.trim().length < 5}>Submit report</button>
        </form>}
      </>}
    </div>
    <ConfirmDialog
      open={blockConfirmOpen}
      title="Block this student?"
      description="They will no longer appear in your discovery, notifications, or chat. You can manage blocked students from your profile."
      confirmLabel="Block student"
      busy={busy}
      onConfirm={block}
      onCancel={() => setBlockConfirmOpen(false)}
    />
  </>;
}
