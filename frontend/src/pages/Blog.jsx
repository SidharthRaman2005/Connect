import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api, resolveMediaUrl } from "../lib/api";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "../components/ToastProvider";
import ConfirmDialog from "../components/ConfirmDialog";

const EMPTY = { title: "", content: "" };

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

  const load = useCallback(async (announce = false) => {
    announce ? setRefreshing(true) : setLoading(true);
    try {
      const data = (await api.get("/blogs")) || [];
      // Newest first, so a freshly published blog always appears on top.
      setPosts([...data].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0)));
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
  }, [toast]);

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
      if (editingId) await api.put(`/blogs/${editingId}`, { title: form.title.trim(), content: form.content.trim() });
      else await api.post("/blogs", { title: form.title.trim(), content: form.content.trim() });
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
    setForm({ title: post.title, content: post.content });
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
          <button className="btn btn--secondary" type="button" onClick={() => load(true)} disabled={refreshing || loading}>
            <span aria-hidden="true">⟳</span> {refreshing ? "Refreshing…" : "Refresh"}
          </button>
          <button className="btn btn--primary" type="button" onClick={openCompose}>Create blog <span aria-hidden="true">+</span></button>
        </div>
      </div>

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
            <div className="blog-compose__actions">
              <button className="btn btn--primary" type="submit" disabled={saving}>{saving ? "Publishing…" : editingId ? "Save changes" : "Publish blog"} <span aria-hidden="true">→</span></button>
              <button className="btn btn--ghost" type="button" onClick={closeCompose}>Cancel</button>
            </div>
          </form>
        </section>
      )}

      <div className="blog-feed-heading">
        <h2>Latest blogs</h2>
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

function BlogCard({ post, isOwner, onEdit, onDelete }) {
  const author = post.author;
  const date = post.createdAt ? new Date(post.createdAt).toLocaleDateString([], { month: "short", day: "numeric", year: "numeric" }) : "";
  const edited = post.updatedAt && new Date(post.updatedAt).getTime() - new Date(post.createdAt).getTime() > 1000;
  return (
    <article className="blog-card">
      <div className="blog-card__author">
        {author.pictureUrl ? <img src={resolveMediaUrl(author.pictureUrl)} alt="" referrerPolicy="no-referrer" /> : <span className="blog-card__avatar">{(author.name || "?").charAt(0)}</span>}
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
      {date && <small className="blog-card__date">{edited ? "Edited" : "Published"} {date}</small>}
    </article>
  );
}
