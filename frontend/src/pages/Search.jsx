import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api, resolveMediaUrl } from "../lib/api";

const PAGE_SIZE = 12;

export default function Search() {
  const [catalog, setCatalog] = useState([]);
  const [q, setQ] = useState("");
  const [branch, setBranch] = useState("");
  const [batch, setBatch] = useState("");
  const [selectedTagIds, setSelectedTagIds] = useState(() => new Set());
  const [tagQuery, setTagQuery] = useState("");
  const [results, setResults] = useState(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    api.get("/tags").then((tags) => setCatalog(tags || [])).catch(() => setCatalog([]));
  }, []);

  const byId = useMemo(() => new Map(catalog.map((tag) => [tag.id, tag])), [catalog]);

  const runSearch = useCallback(async (nextPage = 0) => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams();
      if (q.trim()) params.set("q", q.trim());
      if (branch.trim()) params.set("branch", branch.trim());
      if (batch.trim()) params.set("batch", batch.trim());
      for (const id of selectedTagIds) params.append("tagIds", id);
      params.set("page", nextPage);
      params.set("size", PAGE_SIZE);
      const response = await api.get(`/students/search?${params.toString()}`);
      setResults(response);
      setPage(nextPage);
    } catch (requestError) {
      setError(requestError.message || "Search failed.");
    } finally {
      setLoading(false);
    }
  }, [q, branch, batch, selectedTagIds]);

  useEffect(() => {
    const timer = setTimeout(async () => {
      setLoading(true);
      try {
        const response = await api.get("/students/search?page=0&size=12");
        setResults(response);
        setPage(0);
      } catch (requestError) {
        setError(requestError.message || "Search failed.");
      } finally {
        setLoading(false);
      }
    }, 0);
    return () => clearTimeout(timer);
  }, []);

  const tagSuggestions = useMemo(() => {
    const query = tagQuery.trim().toLowerCase();
    if (!query) return [];
    return catalog
      .filter((tag) => !selectedTagIds.has(tag.id) && tag.name.toLowerCase().includes(query))
      .slice(0, 8);
  }, [tagQuery, catalog, selectedTagIds]);

  const addTag = (id) => {
    setSelectedTagIds((current) => new Set(current).add(id));
    setTagQuery("");
  };

  const removeTag = (id) => {
    setSelectedTagIds((current) => {
      const next = new Set(current);
      next.delete(id);
      return next;
    });
  };

  const connect = async (id) => {
    setBusyId(id);
    setError("");
    try {
      await api.post("/connections", { addresseeId: id });
      setResults((current) => current ? {
        ...current,
        content: current.content.map((student) => (
          student.id === id ? { ...student, relation: "PENDING_OUTGOING" } : student
        )),
      } : current);
    } catch (requestError) {
      setError(requestError.message || "Could not send request.");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="search-page">
      <div className="page-heading">
        <div>
          <p className="page-eyebrow">ADVANCED SEARCH</p>
          <h1>Find people.</h1>
          <p className="page-subtitle">Search classmates by name, branch, batch, or shared interests.</p>
        </div>
        <Link className="page-heading__link" to="/discover">Back to discover <span>→</span></Link>
      </div>

      <form className="search-panel" onSubmit={(event) => { event.preventDefault(); runSearch(0); }}>
        <label className="search-name-field">
          <span aria-hidden="true">⌕</span>
          <input
            value={q}
            onChange={(event) => setQ(event.target.value)}
            placeholder="Search by name"
            aria-label="Search by name"
          />
        </label>
        <div className="search-filter-row">
          <input value={branch} onChange={(event) => setBranch(event.target.value)} placeholder="Branch — e.g. CSE" aria-label="Branch" />
          <input value={batch} onChange={(event) => setBatch(event.target.value)} placeholder="Batch — e.g. 26" inputMode="numeric" aria-label="Batch" />
          <button type="submit" disabled={loading}>{loading ? "Searching…" : "Search"} <span>→</span></button>
        </div>

        {selectedTagIds.size > 0 && (
          <div className="search-selected-tags">
            {[...selectedTagIds].map((id) => (
              <span key={id}>{byId.get(id)?.name || id}<button type="button" onClick={() => removeTag(id)} aria-label={`Remove ${byId.get(id)?.name || "tag"}`}>×</button></span>
            ))}
          </div>
        )}

        <div className="search-tag-field">
          <input
            value={tagQuery}
            onChange={(event) => setTagQuery(event.target.value)}
            placeholder="Filter by skills, interests, or clubs"
            aria-label="Filter by tag"
          />
          {tagSuggestions.length > 0 && (
            <div className="search-tag-suggestions">
              {tagSuggestions.map((tag) => (
                <button type="button" key={tag.id} onClick={() => addTag(tag.id)}>
                  <strong>{tag.name}</strong><small>{tag.category}</small>
                </button>
              ))}
            </div>
          )}
        </div>
      </form>

      {error && <p className="page-error" role="alert">{error}</p>}

      {results && (
        <section className="search-results">
          <div className="search-results__heading">
            <h2>People</h2>
            <span>{results.totalElements} found</span>
          </div>
          {results.content.length ? (
            <div className="search-result-list">
              {results.content.map((student) => (
                <SearchResult key={student.id} student={student} busy={busyId === student.id} onConnect={connect} />
              ))}
            </div>
          ) : (
            <div className="search-empty">
              <span aria-hidden="true">⌕</span>
              <h3>No people found</h3>
              <p>Try removing a filter or searching a broader branch or tag.</p>
            </div>
          )}
          {results.totalPages > 1 && (
            <div className="search-pager">
              <button type="button" onClick={() => runSearch(page - 1)} disabled={page <= 0}>← Previous</button>
              <span>Page {page + 1} of {results.totalPages}</span>
              <button type="button" onClick={() => runSearch(page + 1)} disabled={page + 1 >= results.totalPages}>Next →</button>
            </div>
          )}
        </section>
      )}
    </div>
  );
}

function SearchResult({ student, busy, onConnect }) {
  const meta = [student.branch, student.batch && `Batch ${student.batch}`].filter(Boolean).join(" · ");
  const fallback = `https://i.pravatar.cc/120?img=${((Number(student.id) || 1) % 70) + 1}`;
  const picture = student.pictureUrl ? resolveMediaUrl(student.pictureUrl) : fallback;
  return (
    <article className="search-result">
      <Link className="search-result__profile" to={`/students/${student.id}`}>
        <img src={picture} alt={student.name || "Student"} referrerPolicy="no-referrer" />
        <span>
          <strong>{student.name || "Student"}</strong>
          <small>{meta || "VIT-AP student"}</small>
        </span>
      </Link>
      {student.sharedTagCount > 0 && <span className="search-result__match">{student.sharedTagCount} shared</span>}
      <SearchAction student={student} busy={busy} onConnect={onConnect} />
    </article>
  );
}

function SearchAction({ student, busy, onConnect }) {
  if (student.relation === "CONNECTED") return <Link className="search-result__action is-connected" to={`/chat?with=${student.id}`}>Connected <span>→</span></Link>;
  if (student.relation === "PENDING_OUTGOING") return <span className="search-result__action is-requested">Requested</span>;
  if (student.relation === "PENDING_INCOMING") return <Link className="search-result__action is-response" to="/connections">Respond</Link>;
  return <button type="button" className="search-result__action" onClick={() => onConnect(student.id)} disabled={busy}>{busy ? "Sending…" : "Connect"} <span>+</span></button>;
}
