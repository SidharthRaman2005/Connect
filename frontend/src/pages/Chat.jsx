import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api, chatSocketUrl, hasLocalMedia, resolveMediaUrl } from "../lib/api";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "../components/ToastProvider";
import ConfirmDialog from "../components/ConfirmDialog";
import UserSafetyActions from "../components/UserSafetyActions";

const PAGE_SIZE = 30;
const MAX_RECONNECT_ATTEMPTS = 6;

export default function Chat() {
  const { student: me } = useAuth();
  const toast = useToast();
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
  const [includeArchived, setIncludeArchived] = useState(false);
  const [deliveredIds, setDeliveredIds] = useState(() => new Set());
  const [editingId, setEditingId] = useState(null);
  const [editDraft, setEditDraft] = useState("");
  const [menuForId, setMenuForId] = useState(null);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [attempt, setAttempt] = useState(0);

  const socketRef = useRef(null);
  const selectedIdRef = useRef(selectedId);
  const meRef = useRef(me);
  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);
  const scrollModeRef = useRef(null);
  const restoreOffsetRef = useRef(0);
  const attemptsRef = useRef(0);
  const reconnectTimerRef = useRef(null);
  const connectRef = useRef(null);
  const loadConversationsRef = useRef(null);

  useEffect(() => { meRef.current = me; }, [me]);

  const loadConversations = useCallback(async () => {
    try {
      const data = await api.get(`/chat/conversations?includeArchived=${includeArchived}`);
      setConversations(data || []);
      setSelectedId((current) => {
        if (current && data.some((item) => item.other.id === current)) return current;
        const requested = Number(searchParams.get("with"));
        return data.some((item) => item.other.id === requested)
          ? requested
          : (data.find((item) => !item.archived)?.other.id ?? data[0]?.other.id ?? null);
      });
    } catch (caught) {
      setError(caught.message || "Could not load conversations.");
    } finally {
      setLoadingConversations(false);
    }
  }, [includeArchived, searchParams]);

  useEffect(() => { loadConversationsRef.current = loadConversations; }, [loadConversations]);

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

  const markConversationRead = useCallback(async (otherId) => {
    try {
      await api.post(`/chat/${otherId}/read`);
      const socket = socketRef.current;
      if (socket?.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type: "read", recipientId: otherId }));
      }
      setConversations((items) => items.map((item) =>
        item.other.id === otherId ? { ...item, unreadCount: 0 } : item));
      window.dispatchEvent(new Event("connect:chat-unread"));
    } catch {
      // Reading is best-effort; the UI should never break because of it.
    }
  }, []);

  const loadHistory = useCallback(async (studentId, requestedPage = 0, older = false) => {
    if (!studentId) return;
    if (older && messagesContainerRef.current) {
      restoreOffsetRef.current = messagesContainerRef.current.scrollHeight - messagesContainerRef.current.scrollTop;
      scrollModeRef.current = "restore";
    } else {
      scrollModeRef.current = "bottom";
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
      await markConversationRead(studentId);
    } catch (caught) {
      setMessages([]);
      setError(caught.message || "Could not load messages.");
    } finally {
      setLoadingMessages(false);
    }
  }, [markConversationRead]);

  useEffect(() => {
    let active = true;
    queueMicrotask(() => active && loadHistory(selectedId));
    return () => { active = false; };
  }, [selectedId, loadHistory]);

  useLayoutEffect(() => {
    if (!messagesContainerRef.current || !messages.length) return;
    const mode = scrollModeRef.current;
    scrollModeRef.current = null;
    if (mode === "restore") {
      messagesContainerRef.current.scrollTop =
        messagesContainerRef.current.scrollHeight - restoreOffsetRef.current;
    } else if (mode === "keep") {
      return;
    } else {
      messagesContainerRef.current.scrollTop = messagesContainerRef.current.scrollHeight;
    }
  }, [messages]);

  // WebSocket: typed events, exponential backoff, manual retry.
  useEffect(() => {
    let active = true;
    let socket;

    const scheduleReconnect = () => {
      if (!active) return;
      const current = attemptsRef.current;
      if (current >= MAX_RECONNECT_ATTEMPTS) {
        setSocketState("offline");
        return;
      }
      const delay = Math.min(1000 * 2 ** current, 15000) + Math.floor(Math.random() * 400);
      attemptsRef.current = current + 1;
      setAttempt(current + 1);
      reconnectTimerRef.current = setTimeout(connect, delay);
    };

    const connect = async () => {
      if (!active) return;
      try {
        const { ticket } = await api.post("/chat/socket-ticket");
        if (!active) return;
        socket = new WebSocket(chatSocketUrl(ticket));
        socketRef.current = socket;
        socket.onopen = () => {
          if (!active) return;
          attemptsRef.current = 0;
          setAttempt(0);
          setSocketState("connected");
        };
        socket.onmessage = (event) => {
          let incoming;
          try { incoming = JSON.parse(event.data); } catch { return; }
          if (incoming.type === "delivered") {
            scrollModeRef.current = "keep";
            setDeliveredIds((current) => new Set(current).add(incoming.messageId));
            return;
          }
          if (incoming.type === "read") {
            if (incoming.readerId === selectedIdRef.current) {
              scrollModeRef.current = "keep";
              setMessages((previous) => previous.map((message) =>
                message.senderId === meRef.current?.id && message.recipientId === incoming.readerId
                  ? { ...message, read: true } : message));
            }
            return;
          }
          if (incoming.type === "error") {
            setError(incoming.error || "Something went wrong.");
            toast.error(incoming.error || "Message could not be sent.");
            return;
          }
          if (incoming.type !== "message" || !incoming.data) return;
          const message = incoming.data;
          setMessages((previous) => {
            const index = previous.findIndex((item) => item.id === message.id);
            if (index >= 0) {
              const copy = [...previous];
              copy[index] = message;
              return copy;
            }
            const belongs = selectedIdRef.current &&
              (message.senderId === selectedIdRef.current || message.recipientId === selectedIdRef.current);
            if (!belongs) return previous;
            const tempIndex = incoming.clientId
              ? previous.findIndex((item) => item.clientId === incoming.clientId)
              : previous.findIndex((item) =>
                  typeof item.id === "string" && item.id.startsWith("tmp-") &&
                  item.senderId === message.senderId && item.recipientId === message.recipientId &&
                  item.content === message.content);
            if (tempIndex >= 0) {
              const copy = [...previous];
              copy[tempIndex] = message;
              return copy;
            }
            return previous.some((item) => item.id === message.id) ? previous : [...previous, message];
          });
          window.dispatchEvent(new Event("connect:chat-unread"));
          if (message.senderId === selectedIdRef.current) {
            void markConversationRead(selectedIdRef.current);
          }
          loadConversationsRef.current?.();
        };
        socket.onclose = () => {
          if (!active) return;
          setSocketState("reconnecting");
          scheduleReconnect();
        };
        socket.onerror = () => socket.close();
      } catch {
        if (!active) return;
        setSocketState("reconnecting");
        scheduleReconnect();
      }
    };

    connectRef.current = connect;
    connect();
    return () => {
      active = false;
      clearTimeout(reconnectTimerRef.current);
      socket?.close();
    };
  }, [markConversationRead, toast]);

  const retryConnection = () => {
    clearTimeout(reconnectTimerRef.current);
    attemptsRef.current = 0;
    setAttempt(0);
    setSocketState("connecting");
    connectRef.current?.();
  };

  const sendMessage = (event) => {
    event.preventDefault();
    const content = draft.trim();
    const socket = socketRef.current;
    if (!selectedId || !content || socket?.readyState !== WebSocket.OPEN) return;
    const clientId = `c-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    const optimistic = {
      id: `tmp-${Date.now()}`,
      clientId,
      senderId: me?.id,
      recipientId: selectedId,
      content,
      sentAt: new Date().toISOString(),
      editedAt: null,
      deleted: false,
      delivered: false,
      read: false,
    };
    setMessages((previous) => [...previous, optimistic]);
    socket.send(JSON.stringify({ recipientId: selectedId, content, clientId }));
    setDraft("");
  };

  const saveEdit = async () => {
    const content = editDraft.trim();
    if (!editingId || !content) return;
    try {
      const updated = await api.patch(`/chat/messages/${editingId}`, { content });
      scrollModeRef.current = "keep";
      setMessages((previous) => previous.map((message) => message.id === updated.id ? updated : message));
      setEditingId(null);
      setEditDraft("");
      toast.success("Message updated.");
    } catch (caught) {
      setError(caught.message || "Could not update the message.");
      toast.error(caught.message || "Could not update the message.");
    }
  };

  const confirmDelete = async () => {
    if (!pendingDelete) return;
    try {
      if (pendingDelete.kind === "message") {
        const updated = await api.del(`/chat/messages/${pendingDelete.id}`);
        scrollModeRef.current = "keep";
        setMessages((previous) => previous.map((message) => message.id === updated.id ? updated : message));
        toast.success("Message unsent.");
      } else {
        const id = pendingDelete.id;
        await api.del(`/chat/${id}/conversation`);
        setConversations((items) => items.filter((item) => item.other.id !== id));
        if (selectedId === id) setSelectedId(null);
        toast.success("Conversation removed.");
      }
    } catch (caught) {
      setError(caught.message || "Could not complete that action.");
      toast.error(caught.message || "Could not complete that action.");
    } finally {
      setPendingDelete(null);
    }
  };

  const setPreference = async (conversation, patch) => {
    setMenuForId(null);
    try {
      await api.patch(`/chat/${conversation.other.id}/preferences`, patch);
      setConversations((items) => items.map((item) =>
        item.other.id === conversation.other.id ? { ...item, ...patch } : item));
      toast.success(patch.muted !== undefined
        ? (patch.muted ? "Conversation muted." : "Conversation unmuted.")
        : (patch.archived ? "Conversation archived." : "Conversation restored."));
      if (patch.archived && selectedId === conversation.other.id) setSelectedId(null);
    } catch (caught) {
      toast.error(caught.message || "Could not update the conversation.");
    }
  };

  const selected = conversations.find((item) => item.other.id === selectedId);
  const visibleMessages = historyForId === selectedId ? messages : [];
  const filteredConversations = useMemo(() => {
    const query = conversationSearch.trim().toLowerCase();
    if (!query) return conversations;
    return conversations.filter((conversation) =>
      `${conversation.other.name || ""} ${conversation.other.branch || ""}`.toLowerCase().includes(query));
  }, [conversationSearch, conversations]);

  const statusLabel = (socketState === "connected" ? "Connected"
    : socketState === "offline" ? "Disconnected" : "Reconnecting");

  return (
    <main className="inbox-page">
      <header className="inbox-header">
        <div>
          <p className="page-eyebrow">CHAT</p>
          <h1>Messages</h1>
        </div>
        <div className="inbox-header__actions">
          <span className={`inbox-status${socketState === "offline" ? " is-offline" : ""}`}>{statusLabel}</span>
          {socketState === "offline" && (
            <button className="btn btn--sm btn--secondary" type="button" onClick={retryConnection}>
              Retry {attempt > 0 && `(${attempt})`}
            </button>
          )}
        </div>
      </header>
      {error && (
        <div role="alert" className="inbox-alert">
          <span>{error}</span>
          <button type="button" aria-label="Dismiss message" onClick={() => setError("")}>×</button>
        </div>
      )}

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
          <button
            className={`inbox-filter${includeArchived ? " is-active" : ""}`}
            type="button"
            onClick={() => setIncludeArchived((value) => !value)}
          >
            {includeArchived ? "Showing archived" : "Show archived"}
          </button>

          {loadingConversations ? <div className="inbox-sidebar-state" role="status"><span className="session-loading__spinner" />Loading chats…</div>
            : conversations.length === 0 ? <div className="inbox-sidebar-state">Connect with a student to start chatting.</div>
            : filteredConversations.length === 0 ? <div className="inbox-sidebar-state">No matching conversations.</div>
            : filteredConversations.map((conversation) => (
              <div className="inbox-conversation-wrap" key={conversation.other.id}>
                <button
                  type="button"
                  onClick={() => setSelectedId(conversation.other.id)}
                  className={`inbox-conversation${selectedId === conversation.other.id ? " is-selected" : ""}`}
                >
                  <Avatar student={conversation.other} />
                  <span className="inbox-conversation__text">
                    <strong>{conversation.other.name || "Student"}</strong>
                    <span className="inbox-preview">
                      {conversation.lastMessage || conversation.other.branch || "Start a conversation"}
                    </span>
                  </span>
                  {conversation.muted && <span className="inbox-muted-icon" title="Muted" aria-label="Muted">◐</span>}
                  {conversation.archived && <span className="inbox-muted-icon" title="Archived" aria-label="Archived">▣</span>}
                  {conversation.unreadCount > 0 && (
                    <span className="inbox-unread">{conversation.unreadCount > 99 ? "99+" : conversation.unreadCount}</span>
                  )}
                </button>
                <button
                  className="conversation-menu-trigger"
                  type="button"
                  aria-label={`Options for ${conversation.other.name || "conversation"}`}
                  onClick={() => setMenuForId(menuForId === conversation.other.id ? null : conversation.other.id)}
                >
                  ⋯
                </button>
                {menuForId === conversation.other.id && (
                  <div className="conversation-menu">
                    <button type="button" onClick={() => setPreference(conversation, { muted: !conversation.muted })}>
                      {conversation.muted ? "Unmute" : "Mute"}
                    </button>
                    <button type="button" onClick={() => setPreference(conversation, { archived: !conversation.archived })}>
                      {conversation.archived ? "Restore" : "Archive"}
                    </button>
                    <button
                      type="button"
                      className="is-danger"
                      onClick={() => { setMenuForId(null); setPendingDelete({ kind: "conversation", id: conversation.other.id }); }}
                    >
                      Delete conversation
                    </button>
                  </div>
                )}
              </div>
            ))}
        </aside>

        <section className="inbox-chat" aria-label="Chat messages">
          {selected ? <>
            <div className="inbox-chat__header">
              <Avatar student={selected.other} />
              <div className="inbox-chat__identity">
                <strong>{selected.other.name || "Student"}</strong>
                <div className="inbox-muted">
                  {[selected.other.branch, selected.other.batch && `Batch ${selected.other.batch}`]
                    .filter(Boolean).join(" · ")}
                </div>
              </div>
              <div className="inbox-chat__actions">
                <Link className="inbox-profile-link" to={`/students/${selected.other.id}`} aria-label="View profile">
                  View profile <span>→</span>
                </Link>
                <UserSafetyActions studentId={selected.other.id} />
              </div>
            </div>

            <div className="inbox-messages" ref={messagesContainerRef}>
              {totalPages > page + 1 && (
                <button type="button" onClick={() => loadHistory(selectedId, page + 1, true)}
                  disabled={loadingMessages} className="inbox-older">Load older messages</button>
              )}
              {loadingMessages && visibleMessages.length === 0 ? <div className="inbox-thread-state" role="status"><span className="session-loading__spinner" />Loading messages…</div>
                : visibleMessages.length === 0 ? <div className="inbox-thread-state inbox-thread-state--empty"><span aria-hidden="true">↗</span><strong>No messages yet</strong><p>Send a message to start the conversation.</p></div>
                : visibleMessages.map((message) => {
                  const mine = message.senderId === me?.id;
                  const editing = editingId === message.id;
                  return (
                    <div key={message.id} className={`inbox-message-row ${mine ? "is-my-message" : "is-their-message"}`}>
                      <div className="inbox-message-stack">
                        <div className={`inbox-bubble ${mine ? "is-my-bubble" : "is-their-bubble"}`}>
                          {editing ? (
                            <div className="inbox-edit-box">
                              <input
                                value={editDraft}
                                onChange={(event) => setEditDraft(event.target.value)}
                                onKeyDown={(event) => event.key === "Enter" && saveEdit()}
                                maxLength={2000}
                                aria-label="Edit message"
                                autoFocus
                              />
                              <button type="button" onClick={saveEdit}>Save</button>
                              <button type="button" onClick={() => { setEditingId(null); setEditDraft(""); }}>Cancel</button>
                            </div>
                          ) : (
                            <div>{message.deleted ? "This message was unsent." : message.content}</div>
                          )}
                          <time>{new Date(message.sentAt).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" })}</time>
                        </div>
                        {mine && !message.deleted && !editing && (
                          <div className="inbox-message-actions">
                            <button type="button" onClick={() => { setEditingId(message.id); setEditDraft(message.content); }}>Edit</button>
                            <button type="button" onClick={() => setPendingDelete({ kind: "message", id: message.id })}>Unsend</button>
                          </div>
                        )}
                        {mine && (
                          <span className="inbox-message-meta">
                            {message.deleted ? "Unsent" : message.read ? "Seen"
                              : (message.delivered || deliveredIds.has(message.id)) ? "Delivered"
                              : typeof message.id === "string" ? "Sending…" : "Sent"}
                            {message.editedAt && !message.deleted ? " · edited" : ""}
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })}
              <div ref={messagesEndRef} />
            </div>

            <form onSubmit={sendMessage} className="inbox-composer">
              <input value={draft} onChange={(event) => setDraft(event.target.value)} maxLength={2000}
                placeholder="Write a message…" aria-label="Message" />
              <button type="submit" disabled={!draft.trim() || socketState !== "connected"}>Send</button>
            </form>
          </> : (
            <div className="inbox-empty-state">
              <span aria-hidden="true">✉</span>
              <strong>{loadingConversations ? "Loading conversations" : "Your conversations"}</strong>
              <p>{loadingConversations ? "Your chats will appear here." : "Choose a connection to start chatting."}</p>
            </div>
          )}
        </section>
      </div>

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        title={pendingDelete?.kind === "message" ? "Unsend this message?" : "Delete this conversation?"}
        description={pendingDelete?.kind === "message"
          ? "The message will be removed for both of you and replaced with an unsent placeholder."
          : "Your chat history with this person will be cleared and hidden from your inbox. Their view is unaffected."}
        confirmLabel={pendingDelete?.kind === "message" ? "Unsend" : "Delete"}
        onConfirm={confirmDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </main>
  );
}

function Avatar({ student }) {
  return hasLocalMedia(student.pictureUrl) ? (
    <img className="inbox-avatar avatar--md" src={resolveMediaUrl(student.pictureUrl)} alt="" referrerPolicy="no-referrer" />
  ) : (
    <span className="inbox-avatar inbox-avatar--empty avatar--md">{(student.name || "?").charAt(0)}</span>
  );
}
