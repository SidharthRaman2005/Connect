import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, resolveMediaUrl } from "../lib/api";
import { useAuth } from "../auth/AuthContext";

export default function Dashboard() {
  const { student } = useAuth();
  const navigate = useNavigate();
  const [people, setPeople] = useState([]);
  const [collabs, setCollabs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    Promise.all([api.get("/discover"), api.get("/collabs")])
      .then(([discover, opportunities]) => {
        if (!active) return;
        setPeople((discover || []).slice(0, 3));
        setCollabs((opportunities || []).slice(0, 3));
      })
      .catch((requestError) => {
        if (active) setError(requestError.message || "Could not load your home feed.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, []);

  const firstName = student?.name?.split(" ")[0] || "there";

  return (
    <div className="dashboard-page">
      <div className="dashboard-heading">
        <div>
          <p className="dashboard-eyebrow">YOUR CONNECT SPACE</p>
          <h1>Hey, {firstName} <span aria-hidden="true">👋</span></h1>
          <p className="dashboard-subtitle">A good project starts with the right people.</p>
        </div>
        <span className="dashboard-notifications">Updates are in the bell <span aria-hidden="true">⌁</span></span>
      </div>

      <section className="dashboard-hero">
        <div>
          <span className="dashboard-hero__label">START SOMETHING</span>
          <h2>Make something<br />together.</h2>
          <p>Find teammates, share what you are building, and make campus connections.</p>
        </div>
        <div className="dashboard-hero__actions">
          <button type="button" className="dashboard-hero__secondary" onClick={() => navigate("/collabs")}>Explore opportunities <span>→</span></button>
        </div>
      </section>

      {error && <p className="dashboard-error" role="alert">{error}</p>}
      {loading ? (
        <div className="dashboard-loading"><div className="session-loading__spinner" /><span>Finding your people…</span></div>
      ) : (
        <div className="dashboard-sections">
          <DashboardSection title="People to meet" action="See all" to="/search">
            {people.length ? people.map((person) => <PersonRow key={person.id} person={person} onClick={() => navigate(`/students/${person.id}`)} />) : <EmptyState text="Add interests to your profile to get better suggestions." to="/profile" action="Update profile" />}
          </DashboardSection>
          <DashboardSection title="Open collaborations" action="Explore" to="/collabs">
            {collabs.length ? collabs.map((collab) => <CollabRow key={collab.id} collab={collab} onClick={() => navigate("/collabs")} />) : <EmptyState text="No open opportunities yet. Post the first one." to="/collabs/add" action="Create opportunity" />}
          </DashboardSection>
        </div>
      )}
    </div>
  );
}

function DashboardSection({ title, action, to, children }) {
  return (
    <section className="dashboard-section">
      <div className="dashboard-section__heading"><h2>{title}</h2><Link to={to}>{action} <span>→</span></Link></div>
      <div className="dashboard-list">{children}</div>
    </section>
  );
}

function PersonRow({ person, onClick }) {
  const meta = [person.branch || "VIT-AP", person.batch && `Batch ${person.batch}`].filter(Boolean).join(" · ");
  return (
    <button type="button" className="dashboard-row" onClick={onClick}>
      {person.pictureUrl ? <img src={resolveMediaUrl(person.pictureUrl)} alt="" referrerPolicy="no-referrer" /> : <span className="dashboard-row__avatar">{(person.name || "?").charAt(0)}</span>}
      <span className="dashboard-row__content"><strong>{person.name}</strong><small>{meta}</small></span>
      {person.sharedTagCount > 0 && <span className="dashboard-row__match">{person.sharedTagCount} shared</span>}
      <span className="dashboard-row__arrow" aria-hidden="true">→</span>
    </button>
  );
}

function CollabRow({ collab, onClick }) {
  return (
    <button type="button" className="dashboard-row dashboard-row--collab" onClick={onClick}>
      <span className="dashboard-collab-icon" aria-hidden="true">✦</span>
      <span className="dashboard-row__content"><strong>{collab.title}</strong><small>{collab.type} · by {collab.owner?.name || "Student"}</small></span>
      <span className="dashboard-row__arrow" aria-hidden="true">→</span>
    </button>
  );
}

function EmptyState({ text, action, to }) {
  return <div className="dashboard-empty"><p>{text}</p><Link to={to}>{action} →</Link></div>;
}
