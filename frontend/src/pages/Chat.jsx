import { useCallback, useEffect, useMemo, useRef, useState } from "react";
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
  const [conversationSearch, setConversationSearch] = useState("");
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
  const filteredConversations = useMemo(() => {
    const query = conversationSearch.trim().toLowerCase();
    if (!query) return conversations;
    return conversations.filter((conversation) => {
      const name = conversation.other.name || "";
      const branch = conversation.other.branch || "";
      return `${name} ${branch}`.toLowerCase().includes(query);
    });
  }, [conversationSearch, conversations]);

  return (
    <main className="inbox-page">
      <header className="inbox-header">
        <div>
          <p className="page-eyebrow">CHAT</p>
          <h1>Messages</h1>
        </div>
        <span className="inbox-status">{socketState === "connected" ? "Connected" : "Reconnecting"}</span>
      </header>
      {error && <p role="alert" className="page-error">{error}</p>}
      <div className="inbox-layout">
        <aside className="inbox-sidebar" aria-label="Conversations">
          <div className="inbox-sidebar__heading">
            <h2>Chats</h2>
            <span>{conversations.length}</span>
          </div>
          <label className="inbox-search">
            <span aria-hidden="true">⌕</span>
            <input
              value={conversationSearch}
              onChange={(event) => setConversationSearch(event.target.value)}
              placeholder="Search conversations"
              aria-label="Search conversations"
            />
          </label>
          {loadingConversations ? <p className="inbox-muted">Loading…</p> : conversations.length === 0 ? (
            <p className="inbox-muted">Connect with a student to start chatting.</p>
          ) : filteredConversations.length === 0 ? (
            <p className="inbox-muted">No matching conversations.</p>
          ) : filteredConversations.map((conversation) => (
            <button key={conversation.other.id} type="button" onClick={() => setSelectedId(conversation.other.id)}
              className={`inbox-conversation${selectedId === conversation.other.id ? " is-selected" : ""}`}>
              <Avatar student={conversation.other} />
              <span className="inbox-conversation__text">
                <strong>{conversation.other.name || "Student"}</strong>
                <span className="inbox-preview">{conversation.lastMessage || conversation.other.branch || "Start a conversation"}</span>
              </span>
              {conversation.unreadCount > 0 && <span className="inbox-unread">{conversation.unreadCount > 99 ? "99+" : conversation.unreadCount}</span>}
            </button>
          ))}
        </aside>

        <section className="inbox-chat" aria-label="Chat messages">
          {selected ? <>
            <div className="inbox-chat__header">
              <Avatar student={selected.other} size={42} />
              <div className="inbox-chat__identity">
                <strong>{selected.other.name || "Student"}</strong>
                <div className="inbox-muted">{[selected.other.branch, selected.other.batch && `Batch ${selected.other.batch}`].filter(Boolean).join(" · ")}</div>
              </div>
              <Link className="inbox-profile-link" to={`/students/${selected.other.id}`} aria-label="View profile">View profile <span>→</span></Link>
            </div>
            <div className="inbox-messages">
              {totalPages > page + 1 && <button type="button" onClick={() => loadHistory(selectedId, page + 1, true)} disabled={loadingMessages} className="inbox-older">Load older messages</button>}
              {loadingMessages && visibleMessages.length === 0 ? <p className="inbox-muted">Loading messages…</p> : visibleMessages.length === 0 ? <p className="inbox-empty">No messages yet. Say hello.</p> : visibleMessages.map((message) => (
                <div key={message.id} className={`inbox-message-row ${message.senderId === selectedId ? "is-their-message" : "is-my-message"}`}>
                  <div className={`inbox-bubble ${message.senderId === selectedId ? "is-their-bubble" : "is-my-bubble"}`}>
                    <div>{message.content}</div><time>{new Date(message.sentAt).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" })}</time>
                  </div>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>
            <form onSubmit={sendMessage} className="inbox-composer">
              <input value={draft} onChange={(event) => setDraft(event.target.value)} maxLength={2000} placeholder="Write a message…" aria-label="Message" />
              <button type="submit" disabled={!draft.trim() || socketState !== "connected"}>Send</button>
            </form>
          </> : <div className="inbox-empty-state">{loadingConversations ? "Loading conversations…" : "Choose a connection to start chatting."}</div>}
        </section>
      </div>
    </main>
  );
}

function Avatar({ student, size = 44 }) {
  return student.pictureUrl ? <img className="inbox-avatar" src={resolveMediaUrl(student.pictureUrl)} alt="" width={size} height={size} style={{ width: size, height: size }} referrerPolicy="no-referrer" />
    : <span className="inbox-avatar inbox-avatar--empty" style={{ width: size, height: size, fontSize: size * 0.4 }}>{(student.name || "?").charAt(0)}</span>;
}

