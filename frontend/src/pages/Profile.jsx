import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, resolveMediaUrl } from "../lib/api";
import { useAuth } from "../auth/AuthContext";

const CATEGORIES = [
  { key: "SKILL", label: "Skills", hint: "e.g. Python, Guitar, Public Speaking" },
  { key: "INTEREST", label: "Interests", hint: "e.g. Music, Sports, Photography" },
  { key: "CLUB", label: "Clubs", hint: "e.g. Dance Club, IEEE, NSS" },
];
const MAX_SUGGESTIONS = 10;

/**
 * The student's own profile: upload/remove a profile photo and edit the
 * editable fields (M2.1), and pick skill/interest/club tags from the approved
 * catalog via a separate search per category (M2.2 + M2.3). Photo changes apply
 * immediately; Save persists the fields and tag set, then refreshes the session.
 */
export default function Profile() {
  const { refresh } = useAuth();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [catalog, setCatalog] = useState([]); // all approved tags

  const [branch, setBranch] = useState("");
  const [bio, setBio] = useState("");
  const [visibility, setVisibility] = useState("PUBLIC");
  const [pictureUrl, setPictureUrl] = useState("");
  const [selectedTagIds, setSelectedTagIds] = useState(() => new Set());
  const [queries, setQueries] = useState({ SKILL: "", INTEREST: "", CLUB: "" });

  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [photoError, setPhotoError] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    let active = true;
    Promise.all([api.get("/profile/me"), api.get("/tags")])
      .then(([profile, tags]) => {
        if (!active) return;
        setBranch(profile.branch || "");
        setBio(profile.bio || "");
        setVisibility(profile.visibility || "PUBLIC");
        setPictureUrl(profile.pictureUrl || "");
        setSelectedTagIds(new Set((profile.tags || []).map((t) => t.id)));
        setCatalog(tags || []);
      })
      .catch((e) => active && setLoadError(e.message || "Failed to load your profile."))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, []);

  const byId = useMemo(() => new Map(catalog.map((t) => [t.id, t])), [catalog]);

  const selectedTags = useMemo(
    () => [...selectedTagIds].map((id) => byId.get(id)).filter(Boolean),
    [selectedTagIds, byId]
  );

  const selectedFor = useCallback(
    (category) =>
      selectedTags
        .filter((t) => t.category === category)
        .sort((a, b) => a.name.localeCompare(b.name)),
    [selectedTags]
  );

  const suggestionsFor = useCallback(
    (category) => {
      const q = (queries[category] || "").trim().toLowerCase();
      if (!q) return [];
      return catalog
        .filter(
          (t) =>
            t.category === category &&
            !selectedTagIds.has(t.id) &&
            t.name.toLowerCase().includes(q)
        )
        .slice(0, MAX_SUGGESTIONS);
    },
    [queries, catalog, selectedTagIds]
  );

  const addTag = useCallback((id, category) => {
    setSelectedTagIds((prev) => new Set(prev).add(id));
    setQueries((prev) => ({ ...prev, [category]: "" }));
  }, []);

  const removeTag = useCallback((id) => {
    setSelectedTagIds((prev) => {
      const next = new Set(prev);
      next.delete(id);
      return next;
    });
  }, []);

  const handlePhotoChange = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = ""; // allow re-selecting the same file later
    if (!file) return;
    setPhotoError("");
    setUploadingPhoto(true);
    try {
      const form = new FormData();
      form.append("file", file);
      const updated = await api.post("/profile/me/picture", form);
      setPictureUrl(updated.pictureUrl || "");
      await refresh();
    } catch (err) {
      setPhotoError(err.message || "Upload failed.");
    } finally {
      setUploadingPhoto(false);
    }
  };

  const handlePhotoRemove = async () => {
    setPhotoError("");
    setUploadingPhoto(true);
    try {
      const updated = await api.del("/profile/me/picture");
      setPictureUrl(updated.pictureUrl || "");
      await refresh();
    } catch (err) {
      setPhotoError(err.message || "Remove failed.");
    } finally {
      setUploadingPhoto(false);
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaveError("");
    setSaved(false);
    if (!branch.trim()) {
      setSaveError("Branch is required.");
      return;
    }
    setSaving(true);
    try {
      await api.put("/profile/me", { branch: branch.trim(), bio, visibility });
      await api.put("/profile/me/tags", { tagIds: [...selectedTagIds] });
      await refresh();
      setSaved(true);
    } catch (err) {
      setSaveError(err.message || "Failed to save. Please try again.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div style={{ padding: "2rem", textAlign: "center" }}>Loading…</div>;
  }
  if (loadError) {
    return (
      <div style={styles.page}>
        <p style={styles.error} role="alert">{loadError}</p>
        <Link to="/dashboard">← Back to dashboard</Link>
      </div>
    );
  }

  return (
    <div style={styles.page}>
      <div style={styles.headerRow}>
        <h1 style={{ margin: 0 }}>Your profile</h1>
        <Link to="/dashboard" style={styles.muted}>← Dashboard</Link>
      </div>

      {/* --- Profile photo (upload from device) --- */}
      <div style={styles.photoRow}>
        {pictureUrl ? (
          <img
            src={resolveMediaUrl(pictureUrl)}
            alt="Profile"
            width={72}
            height={72}
            style={styles.avatar}
            referrerPolicy="no-referrer"
          />
        ) : (
          <div style={{ ...styles.avatar, ...styles.avatarEmpty }}>No photo</div>
        )}
        <div style={{ flex: 1 }}>
          <label style={styles.uploadBtn}>
            {uploadingPhoto ? "Uploading…" : pictureUrl ? "Change photo" : "Upload photo"}
            <input
              type="file"
              accept="image/png,image/jpeg,image/webp,image/gif"
              onChange={handlePhotoChange}
              disabled={uploadingPhoto}
              style={{ display: "none" }}
            />
          </label>
          {pictureUrl && (
            <button
              type="button"
              onClick={handlePhotoRemove}
              disabled={uploadingPhoto}
              style={styles.linkBtn}
            >
              Remove photo
            </button>
          )}
          <div style={styles.muted}>PNG, JPEG, WebP, or GIF — up to 2 MB.</div>
          {photoError && <p style={styles.error} role="alert">{photoError}</p>}
        </div>
      </div>

      <form onSubmit={handleSave}>
        <label style={styles.label}>
          Branch *
          <input
            style={styles.input}
            value={branch}
            onChange={(e) => setBranch(e.target.value)}
            placeholder="e.g. CSE"
            maxLength={100}
          />
        </label>

        <label style={styles.label}>
          Bio
          <textarea
            style={{ ...styles.input, minHeight: 90, resize: "vertical" }}
            value={bio}
            onChange={(e) => setBio(e.target.value)}
            placeholder="A short introduction (max 1000 characters)."
            maxLength={1000}
          />
        </label>

        <label style={styles.label}>
          Profile visibility
          <select
            style={styles.input}
            value={visibility}
            onChange={(e) => setVisibility(e.target.value)}
          >
            <option value="PUBLIC">Public — any signed-in student can view</option>
            <option value="PRIVATE">Private — only you can view</option>
          </select>
        </label>

        {/* --- Tags: a separate search per category --- */}
        {CATEGORIES.map(({ key, label, hint }) => {
          const selected = selectedFor(key);
          const suggestions = suggestionsFor(key);
          return (
            <fieldset key={key} style={styles.fieldset}>
              <legend style={styles.legend}>{label}</legend>

              {selected.length > 0 ? (
                <div style={styles.chips}>
                  {selected.map((tag) => (
                    <span key={tag.id} style={styles.chipOn}>
                      {tag.name}
                      <button
                        type="button"
                        onClick={() => removeTag(tag.id)}
                        aria-label={`Remove ${tag.name}`}
                        style={styles.chipRemove}
                      >
                        ×
                      </button>
                    </span>
                  ))}
                </div>
              ) : (
                <p style={styles.muted}>None selected yet.</p>
              )}

              <div style={{ position: "relative", marginTop: "0.6rem" }}>
                <input
                  style={styles.input}
                  value={queries[key]}
                  onChange={(e) => setQueries((prev) => ({ ...prev, [key]: e.target.value }))}
                  placeholder={`Search ${label.toLowerCase()} — ${hint}`}
                />
                {suggestions.length > 0 && (
                  <ul style={styles.suggestions}>
                    {suggestions.map((tag) => (
                      <li key={tag.id}>
                        <button
                          type="button"
                          onClick={() => addTag(tag.id, key)}
                          style={styles.suggestion}
                        >
                          {tag.name}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
                {queries[key].trim() && suggestions.length === 0 && (
                  <p style={styles.muted}>No matches.</p>
                )}
              </div>
            </fieldset>
          );
        })}

        {saveError && <p style={styles.error} role="alert">{saveError}</p>}
        {saved && <p style={styles.success}>Saved.</p>}

        <div style={{ display: "flex", gap: "0.75rem", marginTop: "1rem" }}>
          <button type="submit" disabled={saving} style={styles.primary}>
            {saving ? "Saving…" : "Save profile"}
          </button>
          <button type="button" onClick={() => navigate("/dashboard")} style={styles.secondary}>
            Done
          </button>
        </div>
      </form>
    </div>
  );
}

const styles = {
  page: { maxWidth: 640, margin: "0 auto", padding: "2rem" },
  headerRow: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "baseline",
    marginBottom: "1.5rem",
  },
  photoRow: { display: "flex", gap: "1rem", alignItems: "flex-start", marginBottom: "1.5rem" },
  avatar: { borderRadius: "50%", objectFit: "cover", flexShrink: 0 },
  avatarEmpty: {
    width: 72,
    height: 72,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    background: "#f0f0f0",
    color: "#999",
    fontSize: "0.75rem",
  },
  uploadBtn: {
    display: "inline-block",
    padding: "0.45rem 1rem",
    border: "1px solid #1a73e8",
    color: "#1a73e8",
    borderRadius: 8,
    cursor: "pointer",
    fontSize: "0.9rem",
    fontWeight: 600,
  },
  label: { display: "block", marginBottom: "1rem", fontWeight: 600 },
  input: {
    display: "block",
    width: "100%",
    marginTop: "0.35rem",
    padding: "0.5rem 0.65rem",
    fontSize: "1rem",
    fontWeight: 400,
    border: "1px solid #ccc",
    borderRadius: 8,
    boxSizing: "border-box",
  },
  fieldset: { border: "1px solid #e2e2e2", borderRadius: 10, padding: "1rem", margin: "1rem 0" },
  legend: { fontWeight: 700, padding: "0 0.4rem" },
  chips: { display: "flex", flexWrap: "wrap", gap: "0.5rem" },
  chipOn: {
    display: "inline-flex",
    alignItems: "center",
    gap: "0.35rem",
    padding: "0.35rem 0.4rem 0.35rem 0.75rem",
    borderRadius: 999,
    background: "#1a73e8",
    color: "#fff",
    fontSize: "0.9rem",
  },
  chipRemove: {
    border: "none",
    background: "rgba(255,255,255,0.25)",
    color: "#fff",
    borderRadius: "50%",
    width: 18,
    height: 18,
    lineHeight: "16px",
    cursor: "pointer",
    fontSize: "0.9rem",
    padding: 0,
  },
  suggestions: {
    listStyle: "none",
    margin: "0.25rem 0 0",
    padding: "0.25rem",
    border: "1px solid #ddd",
    borderRadius: 8,
    boxShadow: "0 4px 12px rgba(0,0,0,0.08)",
    maxHeight: 220,
    overflowY: "auto",
    background: "#fff",
  },
  suggestion: {
    display: "block",
    width: "100%",
    padding: "0.45rem 0.6rem",
    border: "none",
    borderRadius: 6,
    background: "transparent",
    cursor: "pointer",
    fontSize: "0.95rem",
    textAlign: "left",
  },
  primary: {
    padding: "0.55rem 1.2rem",
    fontSize: "1rem",
    border: "none",
    borderRadius: 8,
    background: "#1a73e8",
    color: "#fff",
    cursor: "pointer",
  },
  secondary: {
    padding: "0.55rem 1.2rem",
    fontSize: "1rem",
    border: "1px solid #ccc",
    borderRadius: 8,
    background: "#fff",
    cursor: "pointer",
  },
  linkBtn: {
    border: "none",
    background: "none",
    color: "#c0271c",
    cursor: "pointer",
    padding: 0,
    marginLeft: "0.75rem",
    fontSize: "0.9rem",
  },
  muted: { color: "#888", margin: "0.4rem 0 0", fontSize: "0.85rem" },
  error: { color: "#c0271c", marginTop: "0.5rem" },
  success: { color: "#188038", marginTop: "0.5rem" },
};
