import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import StudentCard from "../components/StudentCard";

const PAGE_SIZE = 12;

/**
 * People search (M3.1): filter the directory by name, branch, batch, and tags,
 * with pagination. Connecting is done inline on each result card (M3.2).
 */
export default function Search() {
  const [catalog, setCatalog] = useState([]);
  const [q, setQ] = useState("");
  const [branch, setBranch] = useState("");
  const [batch, setBatch] = useState("");
  const [selectedTagIds, setSelectedTagIds] = useState(() => new Set());
  const [tagQuery, setTagQuery] = useState("");

  const [results, setResults] = useState(null); // PageResponse or null
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    api.get("/tags").then((t) => setCatalog(t || [])).catch(() => setCatalog([]));
  }, []);

  const byId = useMemo(() => new Map(catalog.map((t) => [t.id, t])), [catalog]);

  const runSearch = useCallback(
    async (nextPage = 0) => {
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
        const res = await api.get(`/students/search?${params.toString()}`);
        setResults(res);
        setPage(nextPage);
      } catch (e) {
        setError(e.message || "Search failed.");
      } finally {
        setLoading(false);
      }
    },
    [q, branch, batch, selectedTagIds]
  );

  // Initial load: show everyone (deferred so it doesn't setState during the effect).
  useEffect(() => {
    const t = setTimeout(() => runSearch(0), 0);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const tagSuggestions = useMemo(() => {
    const query = tagQuery.trim().toLowerCase();
    if (!query) return [];
    return catalog
      .filter((t) => !selectedTagIds.has(t.id) && t.name.toLowerCase().includes(query))
      .slice(0, 8);
  }, [tagQuery, catalog, selectedTagIds]);

  const addTag = (id) => {
    setSelectedTagIds((prev) => new Set(prev).add(id));
    setTagQuery("");
  };
  const removeTag = (id) =>
    setSelectedTagIds((prev) => {
      const next = new Set(prev);
      next.delete(id);
      return next;
    });

  const connect = async (id) => {
    setBusyId(id);
    try {
      await api.post("/connections", { addresseeId: id });
      setResults((prev) =>
        prev
          ? {
              ...prev,
              content: prev.content.map((c) =>
                c.id === id ? { ...c, relation: "PENDING_OUTGOING" } : c
              ),
            }
          : prev
      );
    } catch (e) {
      setError(e.message || "Could not send request.");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.headerRow}>
        <h1 style={{ margin: 0 }}>Find people</h1>
        <Link to="/dashboard" style={styles.muted}>← Dashboard</Link>
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          runSearch(0);
        }}
      >
        <div style={styles.filters}>
          <input style={styles.input} value={q} onChange={(e) => setQ(e.target.value)} placeholder="Name" />
          <input style={styles.input} value={branch} onChange={(e) => setBranch(e.target.value)} placeholder="Branch (e.g. CSE)" />
          <input
            style={styles.input}
            value={batch}
            onChange={(e) => setBatch(e.target.value)}
            placeholder="Batch (e.g. 26)"
            inputMode="numeric"
          />
        </div>

        {selectedTagIds.size > 0 && (
          <div style={styles.chips}>
            {[...selectedTagIds].map((id) => (
              <span key={id} style={styles.chipOn}>
                {byId.get(id)?.name || id}
                <button type="button" onClick={() => removeTag(id)} style={styles.chipRemove} aria-label="Remove">×</button>
              </span>
            ))}
          </div>
        )}

        <div style={{ position: "relative", marginTop: "0.6rem" }}>
          <input
            style={styles.input}
            value={tagQuery}
            onChange={(e) => setTagQuery(e.target.value)}
            placeholder="Filter by tag — search skills, interests, clubs"
          />
          {tagSuggestions.length > 0 && (
            <ul style={styles.suggestions}>
              {tagSuggestions.map((t) => (
                <li key={t.id}>
                  <button type="button" onClick={() => addTag(t.id)} style={styles.suggestion}>
                    {t.name} <span style={styles.suggestionCat}>{t.category}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <button type="submit" style={styles.search} disabled={loading}>
          {loading ? "Searching…" : "Search"}
        </button>
      </form>

      {error && <p style={styles.error} role="alert">{error}</p>}

      {results && (
        <>
          <p style={{ color: "#777" }}>{results.totalElements} student(s) found.</p>
          <div style={styles.grid}>
            {results.content.map((card) => (
              <StudentCard key={card.id} card={card} onConnect={connect} busy={busyId === card.id} />
            ))}
          </div>
          {results.totalPages > 1 && (
            <div style={styles.pager}>
              <button onClick={() => runSearch(page - 1)} disabled={page <= 0} style={styles.pageBtn}>
                ← Prev
              </button>
              <span style={{ color: "#777" }}>
                Page {page + 1} of {results.totalPages}
              </span>
              <button
                onClick={() => runSearch(page + 1)}
                disabled={page + 1 >= results.totalPages}
                style={styles.pageBtn}
              >
                Next →
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

const styles = {
  page: { maxWidth: 760, margin: "0 auto", padding: "2rem" },
  headerRow: { display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: "1.25rem" },
  filters: { display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "0.5rem" },
  input: {
    width: "100%",
    padding: "0.5rem 0.65rem",
    fontSize: "1rem",
    border: "1px solid #ccc",
    borderRadius: 8,
    boxSizing: "border-box",
  },
  chips: { display: "flex", flexWrap: "wrap", gap: "0.5rem", marginTop: "0.6rem" },
  chipOn: {
    display: "inline-flex",
    alignItems: "center",
    gap: "0.35rem",
    padding: "0.3rem 0.4rem 0.3rem 0.7rem",
    borderRadius: 999,
    background: "#1a73e8",
    color: "#fff",
    fontSize: "0.85rem",
  },
  chipRemove: {
    border: "none",
    background: "rgba(255,255,255,0.25)",
    color: "#fff",
    borderRadius: "50%",
    width: 18,
    height: 18,
    cursor: "pointer",
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
    position: "absolute",
    zIndex: 5,
    width: "100%",
    boxSizing: "border-box",
  },
  suggestion: { display: "block", width: "100%", padding: "0.45rem 0.6rem", border: "none", borderRadius: 6, background: "transparent", cursor: "pointer", textAlign: "left" },
  suggestionCat: { fontSize: "0.75rem", color: "#999", textTransform: "uppercase" },
  search: {
    marginTop: "0.75rem",
    padding: "0.55rem 1.4rem",
    border: "none",
    borderRadius: 8,
    background: "#1a73e8",
    color: "#fff",
    cursor: "pointer",
    fontSize: "1rem",
  },
  grid: { display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))", gap: "1rem" },
  pager: { display: "flex", alignItems: "center", gap: "1rem", justifyContent: "center", marginTop: "1.5rem" },
  pageBtn: { padding: "0.45rem 0.9rem", borderRadius: 8, border: "1px solid #ccc", background: "#fff", cursor: "pointer" },
  muted: { color: "#888", textDecoration: "none" },
  error: { color: "#c0271c" },
};
