import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { useToast } from "./ToastProvider";
import ConfirmDialog from "./ConfirmDialog";

export default function UserSafetyActions({ studentId }) {
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  const [blocked, setBlocked] = useState(false);
  const [blockConfirmOpen, setBlockConfirmOpen] = useState(false);

  useEffect(() => {
    let active = true;
    api.get("/users/blocked")
      .then((items) => active && setBlocked((items || []).some((person) => person.id === studentId)))
      .catch(() => {});
    return () => { active = false; };
  }, [studentId]);

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

  return <>
    <div className="user-safety-actions">
      {blocked ? (
        <button className="btn btn--sm btn--secondary" type="button" onClick={unblock} disabled={busy}>Unblock</button>
      ) : (
        <button className="btn btn--sm btn--secondary" type="button" onClick={() => setBlockConfirmOpen(true)} disabled={busy}>Block</button>
      )}
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
