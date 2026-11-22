package com.connect.dto;

import java.time.Instant;

/**
 * Envelope for everything pushed over the chat WebSocket.
 *
 * <p>Types: {@code message} (a new or updated message), {@code delivered}
 * (the recipient has an open socket), {@code read} (the other party read your
 * messages) and {@code error}.
 */
public record ChatEventDto(
        String type,
        ChatMessageDto data,
        Long messageId,
        Long readerId,
        String clientId,
        String error,
        Instant at) {

    public static ChatEventDto message(ChatMessageDto message) {
        return message(message, null);
    }

    public static ChatEventDto message(ChatMessageDto message, String clientId) {
        return new ChatEventDto("message", message, null, null, clientId, null, Instant.now());
    }

    public static ChatEventDto delivered(Long messageId) {
        return new ChatEventDto("delivered", null, messageId, null, null, null, Instant.now());
    }

    public static ChatEventDto read(Long readerId) {
        return new ChatEventDto("read", null, null, readerId, null, null, Instant.now());
    }

    public static ChatEventDto error(String message) {
        return new ChatEventDto("error", null, null, null, null, message, Instant.now());
    }
}
