import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api, hasLocalMedia, resolveMediaUrl } from "../lib/api";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "../components/ToastProvider";
import ConfirmDialog from "../components/ConfirmDialog";

const CATEGORIES = [
  { key: "SKILL", label: "Skills", hint: "e.g. Python, Guitar, Public Speaking" },
  { key: "INTEREST", label: "Interests", hint: "e.g. Music, Sports, Photography" },
  { key: "CLUB", label: "Clubs", hint: "e.g. Dance Club, IEEE, NSS" },
];
const MAX_SUGGESTIONS = 8;

export default function Profile() {
  const { student, refresh } = useAuth();
  const toast = useToast();

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [catalog, setCatalog] = useState([]);

  const [branch, setBranch] = useState("");
  const [bio, setBio] = useState("");
  const [visibility, setVisibility] = useState("PUBLIC");
  const [pictureUrl, setPictureUrl] = useState("");
  const [selectedTagIds, setSelectedTagIds] = useState(() => new Set());
  const [queries, setQueries] = useState({ SKILL: "", INTEREST: "", CLUB: "" });

  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [photoError, setPhotoError] = useState("");
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState({ type: "", message: "" });
  const [confirmRemove, setConfirmRemove] = useState(false);
  const [blockedUsers, setBlockedUsers] = useState([]);
  const [blockedError, setBlockedError] = useState("");

  useEffect(() => {
    let active = true;
    Promise.all([api.get("/profile/me"), api.get("/tags")])
      .then(([profile, tags]) => {
        if (!active) return;
        setBranch(profile.branch || "");
        setBio(profile.bio || "");
        setVisibility(profile.visibility || "PUBLIC");
        setPictureUrl(profile.pictureUrl || "");
        setSelectedTagIds(new Set((profile.tags || []).map((tag) => tag.id)));
        setCatalog(tags || []);
      })
      .catch((error) => active && setLoadError(error.message || "Failed to load your profile."))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, []);

  useEffect(() => { api.get("/users/blocked").then((items) => setBlockedUsers(items || [])).catch((error) => setBlockedError(error.message || "Could not load blocked users.")); }, []);

  const unblockUser = async (id) => {
    try { await api.del(`/users/${id}/block`); setBlockedUsers((items) => items.filter((item) => item.id !== id)); toast.success("Student unblocked."); }
    catch (error) { toast.error(error.message || "Could not unblock this student."); }
  };

  const byId = useMemo(() => new Map(catalog.map((tag) => [tag.id, tag])), [catalog]);
  const selectedTags = useMemo(
    () => [...selectedTagIds].map((id) => byId.get(id)).filter(Boolean),
    [selectedTagIds, byId]
  );

  const selectedFor = useCallback((category) =>
    selectedTags.filter((tag) => tag.category === category).sort((a, b) => a.name.localeCompare(b.name)),
  [selectedTags]);

  const suggestionsFor = useCallback((category) => {
    const query = (queries[category] || "").trim().toLowerCase();
    if (!query) return [];
    return catalog
      .filter((tag) => tag.category === category && !selectedTagIds.has(tag.id) && tag.name.toLowerCase().includes(query))
      .slice(0, MAX_SUGGESTIONS);
  }, [queries, catalog, selectedTagIds]);

  const addTag = (id, category) => {
    setSelectedTagIds((current) => new Set(current).add(id));
    setQueries((current) => ({ ...current, [category]: "" }));
  };

  const removeTag = (id) => {
    setSelectedTagIds((current) => {
      const next = new Set(current);
      next.delete(id);
      return next;
    });
  };

  const handlePhotoChange = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    setPhotoError("");
    setUploadingPhoto(true);
    try {
      const form = new FormData();
      form.append("file", file);
      const updated = await api.post("/profile/me/picture", form);
      setPictureUrl(updated.pictureUrl || "");
      await refresh();
      toast.success("Profile photo updated.");
    } catch (error) {
      const message = error.message || "Upload failed.";
      setPhotoError(message);
      toast.error(message);
    } finally {
      setUploadingPhoto(false);
    }
  };

  const handlePhotoRemove = async () => {
    setUploadingPhoto(true);
    setPhotoError("");
    try {
      const updated = await api.del("/profile/me/picture");
      setPictureUrl(updated.pictureUrl || "");
      await refresh();
      setConfirmRemove(false);
      toast.success("Profile photo removed.");
    } catch (error) {
      const message = error.message || "Remove failed.";
      setPhotoError(message);
      toast.error(message);
    } finally {
      setUploadingPhoto(false);
    }
  };

  const handleSave = async (event) => {
    event.preventDefault();
    setStatus({ type: "", message: "" });
    if (!branch.trim()) {
      setStatus({ type: "error", message: "Branch is required." });
      return;
    }
    setSaving(true);
    try {
      await api.put("/profile/me", { branch: branch.trim(), bio, visibility });
      await api.put("/profile/me/tags", { tagIds: [...selectedTagIds] });
      await refresh();
      setStatus({ type: "success", message: "Your changes have been saved." });
      toast.success("Profile saved.");
    } catch (error) {
      const message = error.message || "Failed to save. Please try again.";
      setStatus({ type: "error", message });
      toast.error(message);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="loading"><span className="spinner" aria-hidden="true" /> Loading profile…</div>;
  }
  if (loadError) {
    return (
      <div className="profile-page">
        <p className="page-error" role="alert">{loadError}</p>
        <Link className="text-link" to="/dashboard">← Dashboard</Link>
      </div>
    );
  }

  return (
    <div className="profile-page">
      <header className="profile-header">
        <p className="page-eyebrow">ACCOUNT</p>
        <h1>Your profile</h1>
        <p>Keep your details up to date so the right people can find you.</p>
      </header>

      <section className="profile-identity">
        {hasLocalMedia(pictureUrl) ? (
          <img
            className="avatar avatar--lg"
            src={resolveMediaUrl(pictureUrl)}
            alt="Your profile"
            referrerPolicy="no-referrer"
          />
        ) : (
          <span className="avatar avatar--lg">{(student?.name || "S").charAt(0).toUpperCase()}</span>
        )}
        <div className="profile-identity__info">
          <h2>{student?.name || "Student"}</h2>
          <p>{student?.email || "VIT-AP student"}</p>
          <div className="profile-identity__actions">
            <label className="btn btn--secondary">
              {uploadingPhoto ? "Uploading…" : pictureUrl ? "Change photo" : "Upload photo"}
              <input
                type="file"
                accept="image/png,image/jpeg,image/gif"
                onChange={handlePhotoChange}
                disabled={uploadingPhoto}
                hidden
              />
            </label>
            {pictureUrl && (
              <button type="button" className="btn btn--ghost" onClick={() => setConfirmRemove(true)} disabled={uploadingPhoto}>
                Remove photo
              </button>
            )}
          </div>
          <span className="profile-identity__hint">PNG, JPEG or GIF — up to 2 MB. Changes apply immediately.</span>
          {photoError && <span className="profile-identity__error" role="alert">{photoError}</span>}
        </div>
      </section>

      <form onSubmit={handleSave}>
        <section className="profile-section">
          <div className="profile-section__head">
            <h2>About you</h2>
            <span>This is visible to other students.</span>
          </div>
          <div className="profile-section__body">
            <div className="profile-section__body--split">
              <label className="field">
                <span className="field__label">Branch <b>*</b></span>
                <input className="input" value={branch} onChange={(event) => setBranch(event.target.value)} placeholder="e.g. CSE" maxLength={100} />
              </label>
              <label className="field">
                <span className="field__label">Profile visibility</span>
                <select className="select" value={visibility} onChange={(event) => setVisibility(event.target.value)}>
                  <option value="PUBLIC">Public — any signed-in student</option>
                  <option value="PRIVATE">Private — only you</option>
                </select>
              </label>
            </div>
            <label className="field">
              <span className="field__label">Short bio <span className="field__hint">{bio.length}/1000</span></span>
              <textarea className="textarea" value={bio} onChange={(event) => setBio(event.target.value)} placeholder="Tell your campus a little about yourself…" maxLength={1000} />
            </label>
          </div>
        </section>

        <section className="profile-section">
          <div className="profile-section__head">
            <h2>Interests &amp; skills</h2>
            <span>Used to recommend people and opportunities.</span>
          </div>
          <div className="profile-tag-grid">
            {CATEGORIES.map(({ key, label, hint }) => {
              const selected = selectedFor(key);
              const suggestions = suggestionsFor(key);
              return (
                <div className="profile-tags-field" key={key}>
                  <span className="profile-tags-field__label">{label}</span>
                  <div className="profile-tags-field__selected">
                    {selected.length ? selected.map((tag) => (
                      <span className="tag-chip" key={tag.id}>
                        {tag.name}
                        <button type="button" className="tag-chip__remove" onClick={() => removeTag(tag.id)} aria-label={`Remove ${tag.name}`}>×</button>
                      </span>
                    )) : <span className="profile-tags-field__none">None selected</span>}
                  </div>
                  <div className="autocomplete">
                    <input
                      className="input"
                      value={queries[key]}
                      onChange={(event) => setQueries((current) => ({ ...current, [key]: event.target.value }))}
                      placeholder={`Search ${label.toLowerCase()}…`}
                      aria-label={`Search ${label.toLowerCase()}`}
                    />
                    {suggestions.length > 0 && (
                      <ul className="suggestions">
                        {suggestions.map((tag) => (
                          <li key={tag.id}>
                            <button type="button" onClick={() => addTag(tag.id, key)}>{tag.name}</button>
                          </li>
                        ))}
                      </ul>
                    )}
                    {queries[key].trim() && suggestions.length === 0 && (
                      <span className="field__hint">No matches.</span>
                    )}
                  </div>
                  <span className="field__hint">{hint}</span>
                </div>
              );
            })}
          </div>
        </section>

        <footer className="profile-footer">
          <span className={`profile-footer__status${status.type ? ` is-${status.type}` : ""}`} role="status">
            {status.message || "Photo changes save instantly. Use Save changes for your details and tags."}
          </span>
          <button className="btn btn--primary" type="submit" disabled={saving}>
            {saving ? "Saving…" : "Save changes"}
          </button>
        </footer>
      </form>

      <ConfirmDialog
        open={confirmRemove}
        title="Remove your profile photo?"
        description="Your account will fall back to a default avatar. You can upload a new photo any time."
        confirmLabel="Remove photo"
        busy={uploadingPhoto}
        onConfirm={handlePhotoRemove}
        onCancel={() => setConfirmRemove(false)}
      />
      <section className="profile-section blocked-users-section">
        <div className="profile-section__head"><h2>Blocked students</h2><span>Blocked accounts are hidden from discovery, chat, and notifications.</span></div>
        <div className="profile-section__body">
          {blockedError && <p className="page-error">{blockedError}</p>}
          {blockedUsers.length ? blockedUsers.map((person) => <div className="blocked-user-row" key={person.id}><span>{person.name || person.email || "Student"}</span><button type="button" className="btn btn--sm btn--secondary" onClick={() => unblockUser(person.id)}>Unblock</button></div>) : !blockedError && <p className="connection-empty">You have not blocked anyone.</p>}
        </div>
      </section>
    </div>
  );
}
