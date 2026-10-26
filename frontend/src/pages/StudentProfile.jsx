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
  const [highlights, setHighlights] = useState([]);
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
    api
      .get(`/students/${id}/highlights`)
      .then((h) => active && setHighlights(h || []))
      .catch(() => active && setHighlights([]));
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
          <div style={{ textAlign: "center", marginTop: "1rem" }}>
            {profile.pictureUrl ? (
              <img
                src={resolveMediaUrl(profile.pictureUrl)}
                alt={profile.name || "Student"}
                style={{
                  width: 220,
                  height: 220,
                  borderRadius: "50%",
                  objectFit: "cover",
                  boxShadow: "0 6px 20px rgba(0,0,0,0.15)",
                }}
                referrerPolicy="no-referrer"
              />
            ) : (
              <div
                style={{
                  width: 220,
                  height: 220,
                  borderRadius: "50%",
                  margin: "0 auto",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: "5rem",
                  fontWeight: 700,
                  color: "#fff",
                  textTransform: "uppercase",
                  background: "linear-gradient(135deg, #1a73e8, #6aa8ff)",
                }}
              >
                {(profile.name || "?").charAt(0)}
              </div>
            )}
            <h1 style={{ margin: "1rem 0 0" }}>{profile.name || "Student"}</h1>
            <p style={{ margin: "0.25rem 0 0", color: "#555" }}>
              {[profile.branch, profile.batch && `Batch ${profile.batch}`]
                .filter(Boolean)
                .join(" · ")}
            </p>

            <div style={{ marginTop: "1rem" }}>
              <button
                type="button"
                onClick={connect}
                disabled={connecting}
                style={{
                  padding: "0.6rem 1.6rem",
                  border: "none",
                  borderRadius: 10,
                  background: "#1a73e8",
                  color: "#fff",
                  cursor: "pointer",
                  fontWeight: 600,
                  fontSize: "1rem",
                }}
              >
                {connecting ? "Sending…" : "Connect"}
              </button>
              {connectMsg && (
                <div style={{ marginTop: "0.5rem", color: "#555" }}>{connectMsg}</div>
              )}
            </div>
          </div>

          {profile.bio && (
            <p style={{ marginTop: "1.5rem", lineHeight: 1.6, textAlign: "center" }}>{profile.bio}</p>
          )}

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

          {highlights.length > 0 && (
            <div style={{ marginTop: "1.75rem" }}>
              <h2 style={{ fontSize: "1.1rem", borderBottom: "1px solid #eee", paddingBottom: "0.4rem" }}>
                Highlights
              </h2>
              {highlights.map((h) => (
                <div key={h.id} style={{ border: "1px solid #eee", borderRadius: 12, padding: "1rem", marginTop: "0.85rem" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "0.6rem", flexWrap: "wrap" }}>
                    <strong>{h.title}</strong>
                    <span style={{ padding: "0.15rem 0.55rem", borderRadius: 999, fontSize: "0.75rem", fontWeight: 600, background: "#f1f3f4", color: "#555" }}>
                      {h.status === "IN_PROGRESS" ? "In progress" : h.status === "COMPLETED" ? "Completed" : "Idea"}
                    </span>
                  </div>
                  {h.description && <p style={{ color: "#444", lineHeight: 1.5, margin: "0.6rem 0" }}>{h.description}</p>}
                  {h.tags?.length > 0 && (
                    <div style={{ display: "flex", flexWrap: "wrap", gap: "0.35rem", margin: "0.5rem 0" }}>
                      {h.tags.map((t) => (
                        <span key={t} style={{ padding: "0.2rem 0.55rem", borderRadius: 999, background: "#f1f3f4", fontSize: "0.8rem", color: "#444" }}>{t}</span>
                      ))}
                    </div>
                  )}
                  {h.link && <a href={h.link} target="_blank" rel="noreferrer" style={{ color: "#1a73e8", textDecoration: "none", fontSize: "0.9rem" }}>Link ↗</a>}
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
