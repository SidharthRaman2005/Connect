import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, hasLocalMedia, resolveMediaUrl } from "../lib/api";

export default function Discover() {
  const navigate = useNavigate();
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const pageSize = 12;

  const loadStudents = async (nextPage = 0) => {
    setLoading(true);
    setError("");
    try {
      const result = await api.get(`/discover?page=${nextPage}&size=${pageSize}`);
      setStudents(result.content || []);
      setPage(result.page || 0);
      setTotalPages(result.totalPages || 0);
    } catch (requestError) {
      setError(requestError.message || "Could not load student recommendations.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => loadStudents(0), 0);
    return () => clearTimeout(timer);
  }, []);

  const connect = async (id) => {
    setBusyId(id);
    try {
      await api.post("/connections", { addresseeId: id });
      setStudents((current) => current.map((student) => (
        student.id === id ? { ...student, relation: "PENDING_OUTGOING" } : student
      )));
    } catch (requestError) {
      setError(requestError.message || "Could not send connection request.");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="discover-page">
      <div className="page-heading">
        <div>
          <p className="page-eyebrow">DISCOVER</p>
          <h1>Find your people.</h1>
          <p className="page-subtitle">Meet students who share your interests, skills, and ambition.</p>
        </div>
        <Link className="page-heading__link" to="/search">Advanced search <span>→</span></Link>
      </div>

      <div className="discover-toolbar">
        <div>
          <strong>Recommended for you</strong>
          <span>Ranked by shared interests and profile fit</span>
        </div>
        <button type="button" className="btn btn--secondary" onClick={() => loadStudents(page)} disabled={loading}>
          {loading ? "Refreshing…" : "Refresh"}
        </button>
      </div>

      {error && <p className="page-error" role="alert">{error}</p>}
      {loading ? (
        <div className="dashboard-loading"><span className="session-loading__spinner" /> Finding students…</div>
      ) : students.length ? (
        <div className="discover-grid">
          {students.map((student) => (
            <StudentTile
              key={student.id}
              student={student}
              busy={busyId === student.id}
              onConnect={connect}
              onOpen={() => navigate(`/students/${student.id}`)}
            />
          ))}
        </div>
      ) : (
        <div className="empty-state">
          <span className="empty-state__icon" aria-hidden="true">◎</span>
          <h2>No recommendations yet</h2>
          <p>Add more skills and interests to your profile so Connect can find better matches.</p>
          <Link className="btn btn--primary" to="/profile">Update profile <span aria-hidden="true">→</span></Link>
        </div>
      )}
      {!loading && totalPages > 1 && (
        <div className="search-pager" aria-label="Recommendation pages">
          <button type="button" onClick={() => loadStudents(page - 1)} disabled={page <= 0}>← Previous</button>
          <span>Page {page + 1} of {totalPages}</span>
          <button type="button" onClick={() => loadStudents(page + 1)} disabled={page + 1 >= totalPages}>Next →</button>
        </div>
      )}
    </div>
  );
}

function StudentTile({ student, busy, onConnect, onOpen }) {
  const tags = student.tags || [];
  const relation = student.relation;
  return (
    <article className="discover-card">
      <button type="button" className="discover-card__photo" onClick={onOpen}>
        {hasLocalMedia(student.pictureUrl) ? <img src={resolveMediaUrl(student.pictureUrl)} alt={student.name || "Student"} />
          : <span className="avatar-placeholder" aria-label={student.name || "Student"}>{(student.name || "S").charAt(0).toUpperCase()}</span>}
        {student.sharedTagCount > 0 && <b>{student.sharedTagCount} shared</b>}
      </button>
      <div className="discover-card__body">
        <button type="button" className="discover-card__name" onClick={onOpen}>{student.name || "Student"}</button>
        <p>{[student.branch, student.batch && `Batch ${student.batch}`].filter(Boolean).join(" · ") || "VIT-AP student"}</p>
        <div className="discover-card__tags">
          {tags.slice(0, 3).map((tag) => <span key={tag.id}>{tag.name}</span>)}
          {tags.length > 3 && <span>+{tags.length - 3}</span>}
        </div>
        {relation === "CONNECTED" ? (
          <Link className="card-button card-button--connected" to={`/chat?with=${student.id}`}>Connected <span>↗</span></Link>
        ) : relation === "PENDING_OUTGOING" ? (
          <div className="card-button card-button--muted">Request sent</div>
        ) : relation === "PENDING_INCOMING" ? (
          <Link className="card-button card-button--outline" to="/connections">Respond to request</Link>
        ) : (
          <button type="button" className="card-button" onClick={() => onConnect(student.id)} disabled={busy}>
            {busy ? "Sending…" : "Connect"} <span>+</span>
          </button>
        )}
      </div>
    </article>
  );
}
