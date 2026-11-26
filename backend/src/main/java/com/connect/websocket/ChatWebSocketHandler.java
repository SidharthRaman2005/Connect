package com.connect.websocket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.connect.dto.ChatEventDto;
import com.connect.dto.ChatMessageDto;
import com.connect.dto.WsChatRequest;
import com.connect.service.ChatService;
import com.connect.service.NotificationRealtimeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Chat socket protocol. Clients send {@code {type, recipientId, content, clientId}};
 * the server pushes typed {@link ChatEventDto} envelopes:
 * message / delivered / read / error.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long studentId = (Long) session.getAttributes().get("studentId");
        sessions.computeIfAbsent(studentId, ignored -> ConcurrentHashMap.newKeySet()).add(session);

        // Messages that arrived while offline become delivered now, and their
        // senders are told so their UI can move from "Sent" to "Delivered".
        for (ChatMessageDto message : chatService.markDeliveredOnConnect(studentId)) {
            sendToStudent(message.senderId(), ChatEventDto.delivered(message.id()));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage text) {
        Long me = (Long) session.getAttributes().get("studentId");
        try {
            WsChatRequest request = objectMapper.readValue(text.getPayload(), WsChatRequest.class);
            if (request == null || request.recipientId() == null) {
                sendError(session, "Choose a student first.");
                return;
            }
            if (request.isRead()) {
                boolean marked = chatService.markRead(me, request.recipientId());
                if (marked && chatService.canReceiveMessage(request.recipientId(), me)) {
                    sendToStudent(request.recipientId(), ChatEventDto.read(me));
                }
                return;
            }
            boolean recipientOnline = isOnline(request.recipientId());
            ChatMessageDto message = chatService.send(me, request.recipientId(), request.content(), recipientOnline);
            ChatEventDto event = ChatEventDto.message(message, request.clientId());
            sendToStudent(me, event);
            if (chatService.canReceiveMessage(request.recipientId(), me)) sendToStudent(request.recipientId(), event);
        } catch (Exception exception) {
            sendError(session, exception.getMessage() == null ? "Message could not be sent." : exception.getMessage());
        }
    }

    /** Pushes an edited or unsent message to both participants. */
    public void broadcastMessage(ChatMessageDto message) {
        ChatEventDto event = ChatEventDto.message(message);
        sendToStudent(message.senderId(), event);
        if (chatService.canReceiveMessage(message.recipientId(), message.senderId())) {
            sendToStudent(message.recipientId(), event);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void broadcastNotification(NotificationRealtimeEvent event) {
        sendToStudent(event.recipientId(), event.payload());
    }

    public boolean isOnline(Long studentId) {
        Set<WebSocketSession> studentSessions = sessions.get(studentId);
        return studentSessions != null && studentSessions.stream().anyMatch(WebSocketSession::isOpen);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long studentId = (Long) session.getAttributes().get("studentId");
        Set<WebSocketSession> studentSessions = sessions.get(studentId);
        if (studentSessions != null) {
            studentSessions.remove(session);
            if (studentSessions.isEmpty()) sessions.remove(studentId, studentSessions);
        }
    }

    private void sendToStudent(Long studentId, Object event) {
        Set<WebSocketSession> studentSessions = sessions.get(studentId);
        if (studentSessions == null) return;
        studentSessions.forEach(session -> send(session, event));
    }

    private void send(WebSocketSession session, Object payload) {
        if (!session.isOpen()) return;
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            }
        } catch (Exception ignored) {
            // The socket may have closed between the open check and write.
        }
    }

    private void sendError(WebSocketSession session, String message) {
        send(session, ChatEventDto.error(message));
    }
}
