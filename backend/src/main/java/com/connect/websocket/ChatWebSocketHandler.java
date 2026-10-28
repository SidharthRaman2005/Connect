package com.connect.websocket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.connect.dto.ChatMessageDto;
import com.connect.dto.SendChatMessageRequest;
import com.connect.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Small JSON WebSocket protocol: clients send {recipientId, content}. */
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
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage text) {
        Long senderId = (Long) session.getAttributes().get("studentId");
        try {
            SendChatMessageRequest request = objectMapper.readValue(text.getPayload(), SendChatMessageRequest.class);
            ChatMessageDto message = chatService.send(senderId, request.recipientId(), request.content());
            sendToStudent(senderId, message);
            sendToStudent(request.recipientId(), message);
        } catch (Exception exception) {
            sendError(session, exception.getMessage() == null ? "Message could not be sent." : exception.getMessage());
        }
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

    private void sendToStudent(Long studentId, ChatMessageDto message) {
        Set<WebSocketSession> studentSessions = sessions.get(studentId);
        if (studentSessions == null) return;
        studentSessions.forEach(session -> send(session, message));
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
        try {
            synchronized (session) {
                if (session.isOpen()) session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                        java.util.Map.of("error", message))));
            }
        } catch (Exception ignored) {
            // The socket may have closed before the error could be delivered.
        }
    }
}
