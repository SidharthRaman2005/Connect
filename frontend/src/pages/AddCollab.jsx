import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../lib/api";

const TYPE_OPTIONS = [
  { value: "PROJECT", label: "Project" }, { value: "HACKATHON", label: "Hackathon" },
  { value: "RESEARCH", label: "Research" }, { value: "EVENT", label: "Event" }, { value: "OTHER", label: "Other" },
];

export default function AddCollab() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ title: "", description: "", type: "PROJECT", requiredPeople: "1" });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const update = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));
  const create = async (event) => {
    event.preventDefault();
    if (!form.title.trim()) {
      setError("Title is required.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      const createdPost = await api.post("/collabs", { title: form.title.trim(), description: form.description, type: form.type, requiredPeople: Number(form.requiredPeople) });
      setForm({ title: "", description: "", type: "PROJECT", requiredPeople: "1" });
      navigate("/collabs", { state: { createdPost } });
    } catch (requestError) {
      setError(requestError.message || "Failed to publish.");
    } finally {
      setSaving(false);
    }
  };
  return (
    <div className="add-collab-page">
      <div className="page-heading">
        <div>
          <p className="page-eyebrow">YOUR OPPORTUNITY</p>
          <h1>Start something.</h1>
          <p className="page-subtitle">Tell the Connect community what you want to build and who you need.</p>
        </div>
        <div className="page-heading__actions">
          <Link className="page-heading__link" to="/collabs/manage">Your collabs <span>→</span></Link>
          <Link className="page-heading__link" to="/collabs">Browse collabs <span>→</span></Link>
        </div>
      </div>
      <form className="feature-form" onSubmit={create}>
        <label><span>Title <b aria-hidden="true">*</b></span><input name="title" type="text" required value={form.title} onChange={update("title")} placeholder="What are you looking for?" maxLength={150} autoComplete="off" /></label>
        <label><span>Description</span><textarea name="description" value={form.description} onChange={update("description")} placeholder="What are you building, and who would you like to join?" maxLength={2000} /></label>
        <label><span>Opportunity type</span><select name="type" value={form.type} onChange={update("type")}>{TYPE_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
        <label><span>Teammates needed <b aria-hidden="true">*</b></span><input name="requiredPeople" type="number" min="1" max="50" step="1" required value={form.requiredPeople} onChange={update("requiredPeople")} /><small>The post completes when you accept this many people. Open posts expire after 5 days.</small></label>
        {error && <p className="page-error" role="alert">{error}</p>}
        <button className="btn btn--primary" type="submit" disabled={saving}>{saving ? "Publishing…" : "Publish opportunity"} <span>→</span></button>
      </form>
    </div>
  );
}
