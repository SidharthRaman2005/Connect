import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api, chatSocketUrl, resolveMediaUrl } from "../lib/api";

const PAGE_SIZE = 30;

export default function Chat() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [conversations, setConversations] = useState([]);
  const [selectedId, setSelectedId] = useState(() => Number(searchParams.get("with")) || null);
  const [messages, setMessages] = useState([]);
  const [historyForId, setHistoryForId] = useState(null);
  const [draft, setDraft] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loadingConversations, setLoadingConversations] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [socketState, setSocketState] = useState("connecting");
  const [error, setError] = useState("");
  const socketRef = useRef(null);
  const selectedIdRef = useRef(selectedId);
  const messagesEndRef = useRef(null);

  const loadConversations = useCallback(async () => {
    try {
      const data = await api.get("/chat/conversations");
      setConversations(data || []);
      setSelectedId((current) => {
        if (current && data.some((item) => item.other.id === current)) return current;
        const requested = Number(searchParams.get("with"));
        return data.some((item) => item.other.id === requested) ? requested : (data[0]?.other.id ?? null);
      });
    } catch (e) {
      setError(e.message || "Could not load conversations.");
    } finally {
      setLoadingConversations(false);
    }
  }, [searchParams]);

  useEffect(() => {
    let active = true;
    queueMicrotask(() => active && loadConversations());
    return () => { active = false; };
  }, [loadConversations]);

  useEffect(() => {
    selectedIdRef.current = selectedId;
    if (selectedId) setSearchParams({ with: String(selectedId) }, { replace: true });
    else setSearchParams({}, { replace: true });
  }, [selectedId, setSearchParams]);

  const loadHistory = useCallback(async (studentId, requestedPage = 0, older = false) => {
    if (!studentId) return;
    if (!older) {
      setPage(0);
      setTotalPages(0);
    }
    setLoadingMessages(true);
    setError("");
    try {
      const result = await api.get(`/chat/${studentId}/messages?page=${requestedPage}&size=${PAGE_SIZE}`);
      setMessages((previous) => {
        const combined = older ? [...result.content, ...previous] : result.content;
        return [...new Map(combined.map((message) => [message.id, message])).values()];
      });
      setHistoryForId(studentId);
      setPage(result.page);
      setTotalPages(result.totalPages);
      await api.post(`/chat/${studentId}/read`);
      setConversations((previous) => previous.map((item) => item.other.id === studentId
        ? { ...item, unreadCount: 0 } : item));
    } catch (e) {
      setMessages([]);
      setError(e.message || "Could not load messages.");
    } finally {
      setLoadingMessages(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    queueMicrotask(() => active && loadHistory(selectedId));
    return () => { active = false; };
  }, [selectedId, loadHistory]);

  useEffect(() => {
    if (messages.length) messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages.length, selectedId]);

  useEffect(() => {
    let active = true;
    let reconnectTimer;
    let socket;

    const connect = async () => {
      if (!active) return;
      try {
        const { ticket } = await api.post("/chat/socket-ticket");
        if (!active) return;
        socket = new WebSocket(chatSocketUrl(ticket));
        socketRef.current = socket;
        socket.onopen = () => active && setSocketState("connected");
        socket.onmessage = (event) => {
          let message;
          try { message = JSON.parse(event.data); } catch { return; }
          if (message.error) {
            setError(message.error);
            return;
          }
          const currentStudentId = selectedIdRef.current;
          const belongsToSelected = currentStudentId
            && (message.senderId === currentStudentId || message.recipientId === currentStudentId);
          if (belongsToSelected) {
            setMessages((previous) => previous.some((item) => item.id === message.id)
              ? previous : [...previous, message]);
            if (message.senderId === currentStudentId) {
              api.post(`/chat/${currentStudentId}/read`).then(() => {
                setConversations((previous) => previous.map((item) => item.other.id === currentStudentId
                  ? { ...item, unreadCount: 0 } : item));
              }).catch(() => {});
            }
          }
          loadConversations();
        };
        socket.onclose = () => {
          if (!active) return;
          setSocketState("reconnecting");
          reconnectTimer = setTimeout(connect, 1500);
        };
        socket.onerror = () => socket.close();
      } catch {
        if (!active) return;
        setSocketState("reconnecting");
        reconnectTimer = setTimeout(connect, 2500);
      }
    };

    connect();
    return () => {
      active = false;
      clearTimeout(reconnectTimer);
      socket?.close();
    };
  }, [loadConversations]);

  const sendMessage = (event) => {
    event.preventDefault();
    const content = draft.trim();
    const socket = socketRef.current;
    if (!selectedId || !content || socket?.readyState !== WebSocket.OPEN) return;
    socket.send(JSON.stringify({ recipientId: selectedId, content }));
    setDraft("");
  };

  const selected = conversations.find((item) => item.other.id === selectedId);
  const visibleMessages = historyForId === selectedId ? messages : [];

  return (
    <main style={styles.page}>
      <header style={styles.header}>
        <div><h1 style={{ margin: 0 }}>Messages</h1><span style={styles.status}>{socketState === "connected" ? "Live" : "Connecting…"}</span></div>
        <Link to="/dashboard" style={styles.link}>← Dashboard</Link>
      </header>
      {error && <p role="alert" style={styles.error}>{error}</p>}
      <div style={styles.layout}>
        <aside style={styles.sidebar} aria-label="Conversations">
          <h2 style={styles.sideTitle}>Connections</h2>
          {loadingConversations ? <p style={styles.muted}>Loading…</p> : conversations.length === 0 ? (
            <p style={styles.muted}>Connect with a student to start chatting.</p>
          ) : conversations.map((conversation) => (
            <button key={conversation.other.id} type="button" onClick={() => setSelectedId(conversation.other.id)}
              style={{ ...styles.conversation, ...(selectedId === conversation.other.id ? styles.selectedConversation : {}) }}>
              <Avatar student={conversation.other} />
              <span style={styles.conversationText}>
                <strong>{conversation.other.name || "Student"}</strong>
                <span style={styles.preview}>{conversation.lastMessage || conversation.other.branch || "Start a conversation"}</span>
              </span>
              {conversation.unreadCount > 0 && <span style={styles.unread}>{conversation.unreadCount > 99 ? "99+" : conversation.unreadCount}</span>}
            </button>
          ))}
        </aside>

        <section style={styles.chat} aria-label="Chat messages">
          {selected ? <>
            <div style={styles.chatHeader}>
              <Avatar student={selected.other} size={42} />
              <div><strong>{selected.other.name || "Student"}</strong><div style={styles.muted}>{[selected.other.branch, selected.other.batch && `Batch ${selected.other.batch}`].filter(Boolean).join(" · ")}</div></div>
            </div>
            <div style={styles.messageList}>
              {totalPages > page + 1 && <button type="button" onClick={() => loadHistory(selectedId, page + 1, true)} disabled={loadingMessages} style={styles.older}>Load older messages</button>}
              {loadingMessages && visibleMessages.length === 0 ? <p style={styles.muted}>Loading messages…</p> : visibleMessages.length === 0 ? <p style={styles.empty}>No messages yet. Say hello.</p> : visibleMessages.map((message) => (
                <div key={message.id} style={{ ...styles.messageRow, justifyContent: message.senderId === selectedId ? "flex-start" : "flex-end" }}>
                  <div style={{ ...styles.bubble, ...(message.senderId === selectedId ? styles.theirBubble : styles.myBubble) }}>
                    <div>{message.content}</div><time style={styles.time}>{new Date(message.sentAt).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" })}</time>
                  </div>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>
            <form onSubmit={sendMessage} style={styles.composer}>
              <input value={draft} onChange={(event) => setDraft(event.target.value)} maxLength={2000} placeholder="Write a message…" aria-label="Message" />
              <button type="submit" disabled={!draft.trim() || socketState !== "connected"} style={styles.send}>Send</button>
            </form>
          </> : <div style={styles.emptyState}>{loadingConversations ? "Loading conversations…" : "Choose a connection to start chatting."}</div>}
        </section>
      </div>
    </main>
  );
}

function Avatar({ student, size = 44 }) {
  return student.pictureUrl ? <img src={resolveMediaUrl(student.pictureUrl)} alt="" width={size} height={size} style={{ ...styles.avatar, width: size, height: size }} referrerPolicy="no-referrer" />
    : <span style={{ ...styles.avatarEmpty, width: size, height: size, fontSize: size * 0.4 }}>{(student.name || "?").charAt(0)}</span>;
}

const styles = {
  page: { maxWidth: 1000, margin: "0 auto", padding: "1.5rem" },
  header: { display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" },
  status: { color: "#777", fontSize: ".8rem" }, link: { color: "#1a73e8", textDecoration: "none" },
  error: { color: "#c0271c" }, layout: { display: "grid", gridTemplateColumns: "minmax(220px, 300px) 1fr", minHeight: 560, border: "1px solid #e5e7eb", borderRadius: 14, overflow: "hidden", background: "#fff" },
  sidebar: { borderRight: "1px solid #e5e7eb", padding: ".8rem", overflowY: "auto" }, sideTitle: { fontSize: ".95rem", margin: ".2rem .4rem .7rem" },
  conversation: { width: "100%", display: "flex", alignItems: "center", gap: ".65rem", textAlign: "left", background: "transparent", border: 0, borderRadius: 10, padding: ".6rem .4rem", cursor: "pointer" }, selectedConversation: { background: "#eef4ff" },
  conversationText: { minWidth: 0, flex: 1, display: "flex", flexDirection: "column", color: "#222" }, preview: { overflow: "hidden", whiteSpace: "nowrap", textOverflow: "ellipsis", color: "#777", fontSize: ".8rem", marginTop: ".15rem" },
  unread: { minWidth: 20, borderRadius: 20, padding: ".1rem .35rem", color: "white", background: "#1a73e8", textAlign: "center", fontSize: ".75rem" },
  avatar: { borderRadius: "50%", objectFit: "cover", flexShrink: 0 }, avatarEmpty: { borderRadius: "50%", display: "inline-flex", alignItems: "center", justifyContent: "center", background: "#e8eefc", color: "#1a73e8", fontWeight: 700, textTransform: "uppercase", flexShrink: 0 },
  chat: { minWidth: 0, display: "flex", flexDirection: "column" }, chatHeader: { display: "flex", alignItems: "center", gap: ".65rem", padding: ".7rem 1rem", borderBottom: "1px solid #eee" }, muted: { color: "#777", fontSize: ".82rem" },
  messageList: { flex: 1, maxHeight: 470, overflowY: "auto", padding: "1rem", display: "flex", flexDirection: "column", gap: ".45rem" }, messageRow: { display: "flex" }, bubble: { maxWidth: "78%", padding: ".55rem .75rem", borderRadius: 14, overflowWrap: "anywhere" }, theirBubble: { background: "#f1f3f4", color: "#222", borderBottomLeftRadius: 4 }, myBubble: { background: "#1a73e8", color: "white", borderBottomRightRadius: 4 }, time: { display: "block", textAlign: "right", opacity: .7, fontSize: ".68rem", marginTop: ".25rem" },
  composer: { display: "flex", gap: ".5rem", padding: ".75rem", borderTop: "1px solid #eee" }, send: { border: 0, borderRadius: 8, padding: "0 .9rem", color: "white", background: "#1a73e8", cursor: "pointer" }, older: { alignSelf: "center", border: 0, background: "transparent", color: "#1a73e8", cursor: "pointer" }, empty: { color: "#888", textAlign: "center", margin: "auto" }, emptyState: { margin: "auto", padding: "2rem", color: "#888" },
};
