import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, resolveMediaUrl } from "../lib/api";
import { useToast } from "../components/ToastProvider";

const CATEGORY_LABELS = { SKILL: "Skills", INTEREST: "Interests", CLUB: "Clubs" };
const CATEGORY_ICONS = { SKILL: "◆", INTEREST: "✦", CLUB: "◎" };

/** Another student's public profile, by id. Private (403) and missing (404) profiles are surfaced. */
export default function StudentProfile() {
  const { id } = useParams();
  const toast = useToast();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [connecting, setConnecting] = useState(false);
  const [requested, setRequested] = useState(false);

  useEffect(() => {
    let active = true;
    queueMicrotask(() => {
      if (!active) return;
      setLoading(true);
      setRequested(false);
      api
        .get(`/profile/${id}`)
        .then((data) => {
          if (!active) return;
          setProfile(data);
          setError("");
        })
        .catch((caught) => {
          if (!active) return;
          if (caught.status === 403) setError("This profile is private.");
          else if (caught.status === 404) setError("No such student.");
          else setError(caught.message || "Failed to load profile.");
        })
        .finally(() => active && setLoading(false));
    });
    return () => { active = false; };
  }, [id]);

  const groups = useMemo(() => {
    const result = { SKILL: [], INTEREST: [], CLUB: [] };
    for (const tag of profile?.tags || []) (result[tag.category] ||= []).push(tag);
    return result;
  }, [profile]);

  const connect = async () => {
    setConnecting(true);
    try {
      await api.post("/connections", { addresseeId: Number(id) });
      setRequested(true);
      toast.success("Connection request sent.");
    } catch (caught) {
      toast.error(caught.message || "Could not send request.");
    } finally {
      setConnecting(false);
    }
  };

  if (loading) return <div className="loading"><span className="spinner" aria-hidden="true" /> Loading profile…</div>;

  if (error) {
    return (
      <div className="student-profile-page">
        <Link className="back-link" to="/discover">← Back to discover</Link>
        <div className="empty-state">
          <span className="empty-state__icon" aria-hidden="true">◎</span>
          <h2>Profile unavailable</h2>
          <p>{error}</p>
          <Link className="btn btn--secondary" to="/discover">Browse students</Link>
        </div>
      </div>
    );
  }

  const meta = [profile.branch, profile.batch && `Batch ${profile.batch}`].filter(Boolean).join(" · ") || "VIT-AP student";
  const tagCount = (profile.tags || []).length;

  return (
    <div className="student-profile-page">
      <Link className="back-link" to="/discover">← Back to discover</Link>

      <section className="profile-view">
        <div className="profile-view__banner" aria-hidden="true" />
        <div className="profile-view__main">
          {profile.pictureUrl ? (
            <img className="profile-view__avatar" src={resolveMediaUrl(profile.pictureUrl)} alt={profile.name || "Student"} referrerPolicy="no-referrer" />
          ) : (
            <span className="profile-view__avatar profile-view__avatar--fallback">{(profile.name || "?").charAt(0)}</span>
          )}
          <div className="profile-view__identity">
            <span className="pill pill--accent">VIT-AP STUDENT</span>
            <h1>{profile.name || "Student"}</h1>
            <p>{meta}</p>
          </div>
          <div className="profile-view__actions">
            <button className="btn btn--primary" type="button" onClick={connect} disabled={connecting || requested}>
              {requested ? "Request sent ✓" : connecting ? "Sending…" : "Connect"}
            </button>
            <Link className="btn btn--secondary" to="/discover">Discover more</Link>
          </div>
        </div>
        <div className="profile-view__stats">
          <div className="profile-stat"><strong>{profile.branch || "—"}</strong><span>Branch</span></div>
          <div className="profile-stat"><strong>{profile.batch ? `Batch ${profile.batch}` : "—"}</strong><span>Batch</span></div>
          <div className="profile-stat"><strong>{tagCount}</strong><span>{tagCount === 1 ? "Tag" : "Tags"}</span></div>
        </div>
      </section>

      <section className="profile-view-card">
        <div className="section-heading"><h2>About</h2></div>
        <p className="profile-view-card__text">
          {profile.bio || "This student hasn't added a bio yet. Connect with them to learn more."}
        </p>
      </section>

      <section className="profile-view-card">
        <div className="section-heading"><h2>Skills &amp; interests</h2><span>{tagCount} tags</span></div>
        {tagCount ? (
          <div className="profile-view-groups">
            {["SKILL", "INTEREST", "CLUB"].map((category) => groups[category]?.length ? (
              <div className="profile-view-group" key={category}>
                <div className="profile-view-group__label">
                  <span aria-hidden="true">{CATEGORY_ICONS[category]}</span>
                  {CATEGORY_LABELS[category]}
                </div>
                <div className="profile-view-group__tags">
                  {groups[category].map((tag) => <span className="profile-tag" key={tag.id}>{tag.name}</span>)}
                </div>
              </div>
            ) : null)}
          </div>
        ) : (
          <p className="profile-view-card__text">No skills or interests added yet.</p>
        )}
      </section>
    </div>
  );
}
