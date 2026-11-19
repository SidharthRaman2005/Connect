import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { useAuth } from "../auth/AuthContext";

const CATEGORIES = [
  { key: "SKILL", label: "Skills", hint: "What can you build or contribute?" },
  { key: "INTEREST", label: "Interests", hint: "What do you enjoy exploring?" },
  { key: "CLUB", label: "Clubs", hint: "Which communities are you part of?" },
];

export default function Onboarding() {
  const { student, refresh } = useAuth();
  const navigate = useNavigate();
  const [branch, setBranch] = useState(student?.branch || "");
  const [bio, setBio] = useState("");
  const [visibility, setVisibility] = useState("PUBLIC");
  const [catalog, setCatalog] = useState([]);
  const [selectedTagIds, setSelectedTagIds] = useState(() => new Set());
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    Promise.all([api.get("/profile/me"), api.get("/tags")])
      .then(([profile, tags]) => {
        if (!active) return;
        setBranch(profile.branch || "");
        setBio(profile.bio || "");
        setVisibility(profile.visibility || "PUBLIC");
        setSelectedTagIds(new Set((profile.tags || []).map((tag) => tag.id)));
        setCatalog(tags || []);
      })
      .catch((requestError) => {
        if (active) setError(requestError.message || "Could not load profile setup.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const selectedTags = useMemo(
    () => catalog.filter((tag) => selectedTagIds.has(tag.id)),
    [catalog, selectedTagIds]
  );
  const suggestions = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return [];
    return catalog
      .filter((tag) => !selectedTagIds.has(tag.id) && tag.name.toLowerCase().includes(normalized))
      .slice(0, 8);
  }, [catalog, query, selectedTagIds]);

  const toggleTag = (id) => {
    setSelectedTagIds((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!branch.trim()) {
      setError("Branch is required to complete your profile.");
      return;
    }

    setSaving(true);
    setError("");
    try {
      await api.put("/profile/me", {
        branch: branch.trim(),
        bio: bio.trim(),
        visibility,
      });
      await api.put("/profile/me/tags", { tagIds: [...selectedTagIds] });
      await refresh();
      navigate("/dashboard", { replace: true });
    } catch (requestError) {
      setError(requestError.message || "Could not save your profile. Please try again.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <main className="session-loading">
        <div className="session-loading__spinner" aria-hidden="true" />
        <p>Preparing your profile setup…</p>
      </main>
    );
  }

  return (
    <main className="onboarding-page">
      <section className="onboarding-card">
        <div className="onboarding-progress">
          <span className="onboarding-progress__active" />
          <span />
          <span />
        </div>
        <p className="auth-eyebrow">WELCOME TO CONNECT</p>
        <h1>Make it easier<br />to find you.</h1>
        <p className="onboarding-intro">
          Add a few details so classmates can discover your skills, interests, and ideas.
          You can update everything later.
        </p>

        <form onSubmit={handleSubmit} className="onboarding-form">
          <div className="onboarding-identity">
            <div className="onboarding-avatar">{student?.name?.charAt(0) || "S"}</div>
            <div>
              <strong>{student?.name || "Your profile"}</strong>
              <span>{student?.email}</span>
            </div>
          </div>

          <label className="form-field">
            <span>Branch <b>*</b></span>
            <input
              value={branch}
              onChange={(event) => setBranch(event.target.value)}
              placeholder="e.g. Computer Science"
              maxLength={100}
              autoFocus
            />
          </label>

          <label className="form-field">
            <span>Short bio <em>{bio.length}/1000</em></span>
            <textarea
              value={bio}
              onChange={(event) => setBio(event.target.value)}
              placeholder="What are you interested in building?"
              maxLength={1000}
              rows={4}
            />
          </label>

          <fieldset className="onboarding-tags">
            <legend>What are you into?</legend>
            <p>Select a few tags, or search the approved VIT-AP catalog.</p>
            <div className="tag-picker">
              {selectedTags.map((tag) => (
                <button type="button" className="tag-chip tag-chip--selected" key={tag.id} onClick={() => toggleTag(tag.id)}>
                  {tag.name} <span aria-hidden="true">×</span>
                </button>
              ))}
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Search skills, interests, clubs…"
                aria-label="Search profile tags"
              />
            </div>
            {suggestions.length > 0 && (
              <div className="tag-suggestions">
                {suggestions.map((tag) => (
                  <button type="button" key={tag.id} onClick={() => { toggleTag(tag.id); setQuery(""); }}>
                    <span>{tag.name}</span><small>{tag.category.toLowerCase()}</small>
                  </button>
                ))}
              </div>
            )}
            {query && suggestions.length === 0 && <small className="form-help">No matching tags found.</small>}
            <div className="selected-tag-summary">
              {CATEGORIES.map((category) => (
                <span key={category.key}>
                  {category.label}: {selectedTags.filter((tag) => tag.category === category.key).length}
                </span>
              ))}
            </div>
          </fieldset>

          <fieldset className="onboarding-visibility">
            <legend>Who can see your profile?</legend>
            <div className="visibility-options">
              <label className={visibility === "PUBLIC" ? "visibility-option visibility-option--active" : "visibility-option"}>
                <input type="radio" name="visibility" value="PUBLIC" checked={visibility === "PUBLIC"} onChange={(event) => setVisibility(event.target.value)} />
                <span><strong>Everyone</strong><small>Any signed-in student</small></span>
              </label>
              <label className={visibility === "PRIVATE" ? "visibility-option visibility-option--active" : "visibility-option"}>
                <input type="radio" name="visibility" value="PRIVATE" checked={visibility === "PRIVATE"} onChange={(event) => setVisibility(event.target.value)} />
                <span><strong>Private</strong><small>Only you can view your profile</small></span>
              </label>
            </div>
          </fieldset>

          {error && <p className="auth-error" role="alert">{error}</p>}
          <button className="onboarding-submit" type="submit" disabled={saving}>
            {saving ? "Saving your profile…" : "Complete profile →"}
          </button>
        </form>
      </section>
    </main>
  );
}
