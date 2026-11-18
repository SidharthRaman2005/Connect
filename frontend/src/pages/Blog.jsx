import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, resolveMediaUrl } from "../lib/api";
import { useAuth } from "../auth/AuthContext";

const EMPTY = { title: "", content: "" };

export default function Blog() {
  const { student } = useAuth();
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState(EMPTY);
  const [editingId, setEditingId] = useState(null);

  const load = useCallback(async () => {
    try {
      setPosts((await api.get("/blogs")) || []);
      setError("");
    } catch (requestError) {
      setError(requestError.message || "Could not load blogs.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  const update = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));
  const reset = () => { setForm(EMPTY); setEditingId(null); };

  const submit = async (event) => {
    event.preventDefault();
    if (!form.title.trim()) { setError("Blog title is required."); return; }
    if (!form.content.trim()) { setError("Blog content is required."); return; }
    if (!window.confirm(editingId ? "Save changes to this blog?" : "Post this blog for everyone?")) return;
    setSaving(true);
    setError("");
    try {
      if (editingId) await api.put(`/blogs/${editingId}`, { title: form.title.trim(), content: form.content.trim() });
      else await api.post("/blogs", { title: form.title.trim(), content: form.content.trim() });
      reset();
      await load();
    } catch (requestError) {
      setError(requestError.message || "Could not save the blog.");
    } finally {
      setSaving(false);
    }
  };

  const edit = (post) => {
    setEditingId(post.id);
    setForm({ title: post.title, content: post.content });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const remove = async (post) => {
    if (!window.confirm("Delete this blog? This cannot be undone.")) return;
    try {
      await api.del(`/blogs/${post.id}`);
      if (editingId === post.id) reset();
      await load();
    } catch (requestError) {
      setError(requestError.message || "Could not delete the blog.");
    }
  };

  return (
    <div className="blog-page">
      <div className="page-heading">
        <div>
          <p className="page-eyebrow">COMMUNITY STORIES</p>
          <h1>Blogs.</h1>
          <p className="page-subtitle">Read what students are sharing and publish something of your own.</p>
        </div>
      </div>
      {error && <p className="page-error" role="alert">{error}</p>}
      <div className="blog-layout">
        <section>
          <div className="blog-feed-heading"><h2>Latest blogs</h2><span>{posts.length} {posts.length === 1 ? "story" : "stories"}</span></div>
          {loading ? <div className="dashboard-loading"><span className="session-loading__spinner" /> Loading blogs…</div> : posts.length ? (
            <div className="blog-feed">
              {posts.map((post) => (
                <BlogCard
                  key={post.id}
                  post={post}
                  isOwner={post.author.id === student?.id}
                  onEdit={edit}
                  onRemove={remove}
                />
              ))}
            </div>
          ) : <div className="blog-empty">No blogs yet. Be the first to share a story.</div>}
        </section>
        <aside className="blog-compose">
          <h2>{editingId ? "Edit your blog" : "Post a blog"}</h2>
          <form onSubmit={submit}>
            <label>Title<input value={form.title} onChange={update("title")} placeholder="Give your story a title" maxLength={180} /></label>
            <label>Story<textarea value={form.content} onChange={update("content")} placeholder="Share an idea, experience, lesson, or update…" maxLength={10000} /></label>
            <div className="blog-compose__actions">
              <button className="accent-button" type="submit" disabled={saving}>{saving ? "Publishing…" : editingId ? "Save changes" : "Publish blog"} <span>→</span></button>
              {editingId && <button className="small-button" type="button" onClick={reset}>Cancel</button>}
            </div>
          </form>
        </aside>
      </div>
    </div>
  );
}

function BlogCard({ post, isOwner, onEdit, onRemove }) {
  const author = post.author;
  const date = post.createdAt ? new Date(post.createdAt).toLocaleDateString([], { month: "short", day: "numeric", year: "numeric" }) : "";
  return (
    <article className="blog-card">
      <div className="blog-card__author">
        {author.pictureUrl ? <img src={resolveMediaUrl(author.pictureUrl)} alt="" referrerPolicy="no-referrer" /> : <span className="blog-card__avatar">{(author.name || "?").charAt(0)}</span>}
        <div className="blog-card__author-name">
          <Link to={`/students/${author.id}`}><strong>{author.name || "Student"}</strong></Link>
          <small>{[author.branch, author.batch && `Batch ${author.batch}`].filter(Boolean).join(" · ") || date}</small>
        </div>
        {isOwner && <div className="blog-card__actions"><button className="small-button" type="button" onClick={() => onEdit(post)}>Edit</button><button className="small-button small-button--danger" type="button" onClick={() => onRemove(post)}>Delete</button></div>}
      </div>
      <h3>{post.title}</h3>
      <p className="blog-card__content">{post.content}</p>
      {date && <small className="blog-card__author-name" style={{ display: "block", marginTop: 14 }}>{date}</small>}
    </article>
  );
}
