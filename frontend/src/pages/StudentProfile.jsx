import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, resolveMediaUrl } from "../lib/api";

const CATEGORY_LABELS = { SKILL: "Skills", INTEREST: "Interests", CLUB: "Clubs" };

/**
 * Another student's public profile (M2.1), by id. Shows their tags (M2.3).
 * A private profile returns 403 and a missing one 404; both are surfaced.
 */
export default function StudentProfile() {
  const { id } = useParams();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [connecting, setConnecting] = useState(false);
  const [connectMsg, setConnectMsg] = useState("");

  const connect = async () => {
    setConnecting(true);
    setConnectMsg("");
    try {
      await api.post("/connections", { addresseeId: Number(id) });
      setConnectMsg("Connection request sent.");
    } catch (e) {
      setConnectMsg(e.message || "Could not send request.");
    } finally {
      setConnecting(false);
    }
  };

  useEffect(() => {
    let active = true;
    api
      .get(`/profile/${id}`)
      .then((p) => {
        if (!active) return;
        setProfile(p);
        setError("");
      })
      .catch((e) => {
        if (!active) return;
        if (e.status === 403) setError("This profile is private.");
        else if (e.status === 404) setError("No such student.");
        else setError(e.message || "Failed to load profile.");
      })
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [id]);

  if (loading) return <div className="dashboard-loading"><span className="session-loading__spinner" /> Loading profile…</div>;

  const groups = { SKILL: [], INTEREST: [], CLUB: [] };
  for (const t of profile?.tags || []) (groups[t.category] ||= []).push(t);

  return (
    <div className="student-profile-page">
      <Link className="back-link" to="/discover">← Back to discover</Link>

      {error ? (
        <p className="page-error" role="alert">{error}</p>
      ) : (
        <>
          <section className="student-profile-hero">
            {profile.pictureUrl ? (
              <img
                src={resolveMediaUrl(profile.pictureUrl)}
                alt={profile.name || "Student"}
                className="student-profile__photo"
                referrerPolicy="no-referrer"
              />
            ) : (
              <div
                className="student-profile__photo student-profile__photo--fallback"
              >
                {(profile.name || "?").charAt(0)}
              </div>
            )}
            <h1>{profile.name || "Student"}</h1>
            <p className="student-profile__meta">
              {[profile.branch, profile.batch && `Batch ${profile.batch}`]
                .filter(Boolean)
                .join(" · ")}
            </p>

            <div className="student-profile__actions">
              <button
                type="button"
                onClick={connect}
                disabled={connecting}
                className="accent-button"
              >
                {connecting ? "Sending…" : "Connect"}
              </button>
              {connectMsg && (
                <div className="action-message">{connectMsg}</div>
              )}
            </div>
            </section>

          {profile.bio && (
            <p className="student-profile__bio">{profile.bio}</p>
          )}

          {["SKILL", "INTEREST", "CLUB"].map((category) =>
            groups[category]?.length ? (
              <div key={category} className="profile-detail-section">
                <div className="profile-detail-section__label">
                  {CATEGORY_LABELS[category]}
                </div>
                <div className="profile-tags">
                  {groups[category].map((t) => (
                    <span
                      key={t.id}
                      className="profile-tag"
                    >
                      {t.name}
                    </span>
                  ))}
                </div>
              </div>
            ) : null
          )}

        </>
      )}
    </div>
  );
}
