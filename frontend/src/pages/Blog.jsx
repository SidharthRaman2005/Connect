import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api, hasLocalMedia, resolveMediaUrl } from "../lib/api";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "../components/ToastProvider";
import ConfirmDialog from "../components/ConfirmDialog";

const EMPTY = { title: "", content: "", tags: "" };

export default function Blog() {
  const { student } = useAuth();
  const toast = useToast();
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState(EMPTY);
  const [editingId, setEditingId] = useState(null);
  const [composeOpen, setComposeOpen] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [view, setView] = useState("feed");
  const [query, setQuery] = useState("");
  const [tagFilter, setTagFilter] = useState("");

  const load = useCallback(async (announce = false) => {
    announce ? setRefreshing(true) : setLoading(true);
    try {
      const params = new URLSearchParams();
      if (query.trim()) params.set("q", query.trim());
      if (tagFilter.trim()) params.set("tag", tagFilter.trim());
      const data = (await api.get(view === "mine" ? "/blogs/me" : `/blogs?${params.toString()}`)) || [];
      // Newest first, so a freshly published blog always appears on top.
      setPosts([...data].filter((post) => view !== "mine" || (!query || `${post.title} ${post.content}`.toLowerCase().includes(query.toLowerCase())) && (!tagFilter || (post.tags || []).includes(tagFilter.toLowerCase())))
        .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0)));
      setError("");
      if (announce) toast.success("Blogs are up to date.");
    } catch (requestError) {
      const message = requestError.message || "Could not load blogs.";
      setError(message);
      if (announce) toast.error(message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [toast, view, query, tagFilter]);

  useEffect(() => {
    const timer = setTimeout(() => load(), 0);
    return () => clearTimeout(timer);
  }, [load]);

  const update = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));

  const openCompose = () => {
    setForm(EMPTY);
    setEditingId(null);
    setComposeOpen(true);
    setError("");
  };

  const closeCompose = () => {
    setForm(EMPTY);
    setEditingId(null);
    setComposeOpen(false);
  };

  const submit = async (event) => {
    event.preventDefault();
    if (!form.title.trim()) { setError("Blog title is required."); return; }
    if (!form.content.trim()) { setError("Blog content is required."); return; }
    setSaving(true);
    setError("");
    const wasEditing = Boolean(editingId);
    try {
      const payload = { title: form.title.trim(), content: form.content.trim(), tags: parseTags(form.tags) };
      if (editingId) await api.put(`/blogs/${editingId}`, payload);
      else await api.post("/blogs", payload);
      closeCompose();
      await load();
      toast.success(wasEditing ? "Blog updated." : "Your blog is live.");
    } catch (requestError) {
      const message = requestError.message || "Could not save the blog.";
      setError(message);
      toast.error(message);
    } finally {
      setSaving(false);
    }
  };

  const edit = (post) => {
    setEditingId(post.id);
    setForm({ title: post.title, content: post.content, tags: (post.tags || []).join(", ") });
    setComposeOpen(true);
    setError("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const remove = async () => {
    if (!pendingDelete) return;
    setSaving(true);
    try {
      await api.del(`/blogs/${pendingDelete.id}`);
      if (editingId === pendingDelete.id) closeCompose();
      await load();
      toast.success("Blog deleted.");
    } catch (requestError) {
      const message = requestError.message || "Could not delete the blog.";
      setError(message);
      toast.error(message);
    } finally {
      setSaving(false);
      setPendingDelete(null);
    }
  };

  const latestLabel = useMemo(() => {
    if (!posts.length) return "";
    return new Date(posts[0].createdAt).toLocaleDateString([], { month: "short", day: "numeric", year: "numeric" });
  }, [posts]);

  return (
    <div className="blog-page">
      <div className="page-heading">
        <div>
          <p className="page-eyebrow">COMMUNITY STORIES</p>
          <h1>Blogs.</h1>
          <p className="page-subtitle">Read what students are sharing and publish something of your own.</p>
        </div>
        <div className="page-heading__actions">
          <button className={`btn ${view === "feed" ? "btn--secondary" : "btn--ghost"}`} type="button" onClick={() => setView("feed")}>Latest</button>
          <button className={`btn ${view === "mine" ? "btn--secondary" : "btn--ghost"}`} type="button" onClick={() => setView("mine")}>My blogs</button>
          <button className="btn btn--secondary" type="button" onClick={() => load(true)} disabled={refreshing || loading}>
            <span aria-hidden="true">⟳</span> {refreshing ? "Refreshing…" : "Refresh"}
          </button>
          <button className="btn btn--primary" type="button" onClick={openCompose}>Create blog <span aria-hidden="true">+</span></button>
        </div>
      </div>

      <form className="blog-search-row" onSubmit={(event) => { event.preventDefault(); load(); }}>
        <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search blogs" aria-label="Search blogs" />
        <input value={tagFilter} onChange={(event) => setTagFilter(event.target.value.replace(/^#/, ""))} placeholder="Filter by tag" aria-label="Filter blogs by tag" />
        <button className="btn btn--secondary" type="submit">Search</button>
        {(query || tagFilter) && <button className="btn btn--ghost" type="button" onClick={() => { setQuery(""); setTagFilter(""); }}>Clear</button>}
      </form>

      {error && !composeOpen && <p className="page-error" role="alert">{error}</p>}

      {composeOpen && (
        <section className="blog-compose-panel">
          <div className="blog-compose-panel__head">
            <div>
              <p className="page-eyebrow">YOUR STORY</p>
              <h2>{editingId ? "Edit your blog" : "Create a blog"}</h2>
            </div>
            <button className="btn btn--ghost btn--sm" type="button" onClick={closeCompose} aria-label="Close editor">✕</button>
          </div>
          {error && <p className="page-error" role="alert">{error}</p>}
          <form onSubmit={submit}>
            <label className="field">
              <span className="field__label">Title</span>
              <input className="input" value={form.title} onChange={update("title")} placeholder="Give your story a title" maxLength={180} />
            </label>
            <label className="field">
              <span className="field__label">Story</span>
              <textarea className="textarea" value={form.content} onChange={update("content")} placeholder="Share an idea, experience, lesson, or update…" maxLength={10000} />
            </label>
            <label className="field">
              <span className="field__label">Tags <em>up to 8, comma separated</em></span>
              <input className="input" value={form.tags} onChange={update("tags")} placeholder="project, learning, campus" maxLength={360} />
            </label>
            <div className="blog-compose__actions">
              <button className="btn btn--primary" type="submit" disabled={saving}>{saving ? "Publishing…" : editingId ? "Save changes" : "Publish blog"} <span aria-hidden="true">→</span></button>
              <button className="btn btn--ghost" type="button" onClick={closeCompose}>Cancel</button>
            </div>
          </form>
        </section>
      )}

      <div className="blog-feed-heading">
        <h2>{view === "mine" ? "My blogs" : "Latest blogs"}</h2>
        <span>{posts.length} {posts.length === 1 ? "story" : "stories"}{latestLabel ? ` · updated ${latestLabel}` : ""}</span>
      </div>

      {loading ? (
        <div className="loading"><span className="spinner" aria-hidden="true" /> Loading blogs…</div>
      ) : posts.length ? (
        <div className="blog-feed">
          {posts.map((post) => (
            <BlogCard
              key={post.id}
              post={post}
              isOwner={post.author.id === student?.id}
              onEdit={edit}
              onDelete={setPendingDelete}
              onTag={(tag) => { setTagFilter(tag); setView("feed"); }}
              onUpdated={(updated) => setPosts((current) => current.map((item) => item.id === updated.id ? { ...item, ...updated } : item))}
              currentUserId={student?.id}
            />
          ))}
        </div>
      ) : (
        <div className="empty-state">
          <span className="empty-state__icon" aria-hidden="true">▤</span>
          <h2>No blogs yet</h2>
          <p>Be the first to share something with the Connect community.</p>
          <button className="btn btn--primary" type="button" onClick={openCompose}>Create your first blog</button>
        </div>
      )}

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        title="Delete this blog?"
        description="This story will be permanently removed and cannot be recovered."
        confirmLabel="Delete blog"
        busy={saving}
        onConfirm={remove}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  );
}

function BlogCard({ post, isOwner, onEdit, onDelete, onTag, onUpdated, currentUserId }) {
  const [comments, setComments] = useState([]);
  const [commentsOpen, setCommentsOpen] = useState(false);
  const [commentDraft, setCommentDraft] = useState("");
  const [busy, setBusy] = useState(false);
  const toast = useToast();
  const author = post.author;
  const date = post.createdAt ? new Date(post.createdAt).toLocaleDateString([], { month: "short", day: "numeric", year: "numeric" }) : "";
  const edited = post.updatedAt && new Date(post.updatedAt).getTime() - new Date(post.createdAt).getTime() > 1000;
  const toggleLike = async () => {
    setBusy(true);
    try {
      if (post.likedByMe) await api.del(`/blogs/${post.id}/like`);
      else await api.put(`/blogs/${post.id}/like`);
      onUpdated({ ...post, likedByMe: !post.likedByMe, likeCount: Math.max(0, (post.likeCount || 0) + (post.likedByMe ? -1 : 1)) });
    } catch (error) { toast.error(error.message || "Could not update like."); }
    finally { setBusy(false); }
  };
  const toggleComments = async () => {
    const next = !commentsOpen;
    setCommentsOpen(next);
    if (next) {
      try { setComments((await api.get(`/blogs/${post.id}/comments`)) || []); }
      catch (error) { toast.error(error.message || "Could not load comments."); }
    }
  };
  const addComment = async (event) => {
    event.preventDefault();
    if (!commentDraft.trim()) return;
    try {
      const comment = await api.post(`/blogs/${post.id}/comments`, { content: commentDraft.trim() });
      setComments((current) => [...current, comment]);
      setCommentDraft("");
    } catch (error) { toast.error(error.message || "Could not add comment."); }
  };
  const removeComment = async (commentId) => {
    try { await api.del(`/blogs/${post.id}/comments/${commentId}`); setComments((current) => current.filter((item) => item.id !== commentId)); }
    catch (error) { toast.error(error.message || "Could not remove comment."); }
  };
  return (
    <article className="blog-card">
      <div className="blog-card__author">
        {hasLocalMedia(author.pictureUrl) ? <img src={resolveMediaUrl(author.pictureUrl)} alt="" /> : <span className="blog-card__avatar">{(author.name || "?").charAt(0)}</span>}
        <div className="blog-card__author-name">
          <Link to={`/students/${author.id}`}><strong>{author.name || "Student"}</strong></Link>
          <small>{[author.branch, author.batch && `Batch ${author.batch}`].filter(Boolean).join(" · ") || date}</small>
        </div>
        {isOwner && <div className="blog-card__actions">
          <button className="btn btn--sm btn--secondary" type="button" onClick={() => onEdit(post)}>Edit</button>
          <button className="btn btn--sm btn--danger" type="button" onClick={() => onDelete(post)}>Delete</button>
        </div>}
      </div>
      <h3>{post.title}</h3>
      <p className="blog-card__content">{post.content}</p>
      {(post.tags || []).length > 0 && <div className="blog-card__tags">{post.tags.map((tag) => <button type="button" key={tag} onClick={() => onTag(tag)}>#{tag}</button>)}</div>}
      {date && <small className="blog-card__date">{edited ? "Edited" : "Published"} {date}</small>}
      <div className="blog-interactions">
        <button type="button" className={post.likedByMe ? "is-liked" : ""} onClick={toggleLike} disabled={busy}>♥ {post.likeCount || 0}</button>
        <button type="button" onClick={toggleComments}>{commentsOpen ? "Hide comments" : "Comments"}</button>
      </div>
      {commentsOpen && <section className="blog-comments" aria-label="Comments">
        {comments.map((comment) => <div className="blog-comment" key={comment.id}><div><strong>{comment.author.name || "Student"}</strong><small>{new Date(comment.createdAt).toLocaleDateString()}</small><p>{comment.content}</p></div>{(comment.author.id === currentUserId || isOwner) && <button type="button" onClick={() => removeComment(comment.id)} aria-label="Remove comment">×</button>}</div>)}
        <form onSubmit={addComment}><input value={commentDraft} onChange={(event) => setCommentDraft(event.target.value)} maxLength={2000} placeholder="Write a comment…" aria-label="Write a comment" /><button className="btn btn--sm btn--primary" type="submit">Comment</button></form>
      </section>}
    </article>
  );
}

function parseTags(value) {
  return [...new Set((value || "").split(",").map((tag) => tag.trim().replace(/^#/, "").toLowerCase()).filter(Boolean))].slice(0, 8);
}
