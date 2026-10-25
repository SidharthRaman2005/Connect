import { Link } from "react-router-dom";
import { resolveMediaUrl } from "../lib/api";

/**
 * Photo-forward student card: a large profile picture is the focal point, with
 * the name overlaid at the bottom and details plus the connection action below —
 * so you can see the person before deciding to connect.
 */
export default function StudentCard({ card, onConnect, busy }) {
  const tags = card.tags || [];
  const shown = tags.slice(0, 4);
  const meta = [card.branch, card.batch && `Batch ${card.batch}`].filter(Boolean).join(" · ");

  return (
    <div style={styles.card}>
      <Link to={`/students/${card.id}`} style={styles.photoLink}>
        {card.pictureUrl ? (
          <img
            src={resolveMediaUrl(card.pictureUrl)}
            alt={card.name || "Student"}
            style={styles.photo}
            referrerPolicy="no-referrer"
          />
        ) : (
          <div style={styles.photoEmpty}>{(card.name || "?").charAt(0)}</div>
        )}
        <div style={styles.overlay}>
          <div style={styles.name}>{card.name || "Student"}</div>
          {meta && <div style={styles.meta}>{meta}</div>}
        </div>
      </Link>

      <div style={styles.body}>
        {card.sharedTagCount > 0 && (
          <div style={styles.shared}>
            {card.sharedTagCount} shared tag{card.sharedTagCount > 1 ? "s" : ""}
          </div>
        )}
        {shown.length > 0 && (
          <div style={styles.tags}>
            {shown.map((t) => (
              <span key={t.id} style={styles.tag}>{t.name}</span>
            ))}
            {tags.length > shown.length && (
              <span style={styles.more}>+{tags.length - shown.length}</span>
            )}
          </div>
        )}
        <div style={styles.actionRow}>
          <Action card={card} onConnect={onConnect} busy={busy} />
        </div>
      </div>
    </div>
  );
}

function Action({ card, onConnect, busy }) {
  switch (card.relation) {
    case "CONNECTED":
      return <div style={{ ...styles.btn, ...styles.btnOk }}>✓ Connected</div>;
    case "PENDING_OUTGOING":
      return <div style={{ ...styles.btn, ...styles.btnMuted }}>Requested</div>;
    case "PENDING_INCOMING":
      return <Link to="/connections" style={{ ...styles.btn, ...styles.btnRespond }}>Respond to request</Link>;
    default:
      return (
        <button
          type="button"
          onClick={() => onConnect(card.id)}
          disabled={busy}
          style={{ ...styles.btn, ...styles.btnConnect }}
        >
          {busy ? "…" : "Connect"}
        </button>
      );
  }
}

const styles = {
  card: {
    border: "1px solid #e6e6e6",
    borderRadius: 16,
    overflow: "hidden",
    background: "#fff",
    boxShadow: "0 2px 10px rgba(0,0,0,0.06)",
    display: "flex",
    flexDirection: "column",
  },
  photoLink: {
    position: "relative",
    display: "block",
    width: "100%",
    aspectRatio: "3 / 4",
    background: "#f0f0f0",
    textDecoration: "none",
  },
  photo: { width: "100%", height: "100%", objectFit: "cover", display: "block" },
  photoEmpty: {
    width: "100%",
    height: "100%",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    fontSize: "4rem",
    fontWeight: 700,
    color: "#fff",
    textTransform: "uppercase",
    background: "linear-gradient(135deg, #1a73e8, #6aa8ff)",
  },
  overlay: {
    position: "absolute",
    left: 0,
    right: 0,
    bottom: 0,
    padding: "2.5rem 1rem 0.9rem",
    background: "linear-gradient(to top, rgba(0,0,0,0.72), rgba(0,0,0,0))",
    color: "#fff",
  },
  name: { fontSize: "1.25rem", fontWeight: 700, lineHeight: 1.2 },
  meta: { fontSize: "0.85rem", opacity: 0.9, marginTop: "0.15rem" },
  body: { padding: "0.85rem 1rem 1rem", display: "flex", flexDirection: "column", gap: "0.6rem" },
  shared: { fontSize: "0.8rem", color: "#1a73e8", fontWeight: 600 },
  tags: { display: "flex", flexWrap: "wrap", gap: "0.35rem" },
  tag: { padding: "0.2rem 0.55rem", borderRadius: 999, background: "#f1f3f4", fontSize: "0.78rem", color: "#444" },
  more: { fontSize: "0.78rem", color: "#888", alignSelf: "center" },
  actionRow: { marginTop: "0.15rem" },
  btn: {
    display: "block",
    width: "100%",
    textAlign: "center",
    padding: "0.6rem 1rem",
    borderRadius: 10,
    fontSize: "0.95rem",
    fontWeight: 600,
    boxSizing: "border-box",
    textDecoration: "none",
  },
  btnConnect: { border: "none", background: "#1a73e8", color: "#fff", cursor: "pointer" },
  btnMuted: { background: "#f1f3f4", color: "#777" },
  btnOk: { background: "#e6f4ea", color: "#188038" },
  btnRespond: { background: "#fef7e0", color: "#a56b00" },
};
