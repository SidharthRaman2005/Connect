package com.connect.dto;

/** Payload sent by the browser over the chat WebSocket. */
public record WsChatRequest(
        String type,
        Long recipientId,
        String content) {

    public boolean isRead() {
        return "read".equalsIgnoreCase(type);
    }
}
