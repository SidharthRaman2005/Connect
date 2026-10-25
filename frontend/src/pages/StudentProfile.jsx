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

  if (loading) {
    return <div style={{ padding: "2rem", textAlign: "center" }}>Loading…</div>;
  }

  const groups = { SKILL: [], INTEREST: [], CLUB: [] };
  for (const t of profile?.tags || []) (groups[t.category] ||= []).push(t);

  return (
    <div style={{ maxWidth: 640, margin: "0 auto", padding: "2rem" }}>
      <Link to="/dashboard" style={{ color: "#888", textDecoration: "none" }}>← Dashboard</Link>

      {error ? (
        <p style={{ color: "#c0271c", marginTop: "1.5rem" }} role="alert">{error}</p>
      ) : (
        <>
          <div style={{ display: "flex", alignItems: "center", gap: "1rem", marginTop: "1rem" }}>
            {profile.pictureUrl && (
              <img
                src={resolveMediaUrl(profile.pictureUrl)}
                alt=""
                width={56}
                height={56}
                style={{ borderRadius: "50%", objectFit: "cover" }}
                referrerPolicy="no-referrer"
              />
            )}
            <div>
              <h1 style={{ margin: 0 }}>{profile.name || "Student"}</h1>
              <p style={{ margin: "0.25rem 0 0", color: "#555" }}>
                {[profile.branch, profile.batch && `Batch ${profile.batch}`]
                  .filter(Boolean)
                  .join(" · ")}
              </p>
            </div>
          </div>

          {profile.bio && <p style={{ marginTop: "1.25rem", lineHeight: 1.6 }}>{profile.bio}</p>}

          {["SKILL", "INTEREST", "CLUB"].map((category) =>
            groups[category]?.length ? (
              <div key={category} style={{ marginTop: "1.25rem" }}>
                <div style={{ fontSize: "0.8rem", textTransform: "uppercase", color: "#888" }}>
                  {CATEGORY_LABELS[category]}
                </div>
                <div style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem", marginTop: "0.4rem" }}>
                  {groups[category].map((t) => (
                    <span
                      key={t.id}
                      style={{
                        padding: "0.3rem 0.7rem",
                        borderRadius: 999,
                        border: "1px solid #ddd",
                        background: "#f6f8fa",
                        fontSize: "0.9rem",
                      }}
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
