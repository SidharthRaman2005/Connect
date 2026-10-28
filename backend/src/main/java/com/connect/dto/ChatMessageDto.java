package com.connect.dto;

import java.time.Instant;

import com.connect.entity.ChatMessage;

public record ChatMessageDto(Long id, Long senderId, Long recipientId, String content,
                             Instant sentAt, boolean read) {
    public static ChatMessageDto from(ChatMessage message) {
        return new ChatMessageDto(message.getId(), message.getSender().getId(),
                message.getRecipient().getId(), message.getContent(), message.getSentAt(),
                message.getReadAt() != null);
    }
}
