package com.connect.dto;

import java.time.Instant;

import com.connect.entity.ChatMessage;

/** A single message as seen by either participant in a conversation. */
public record ChatMessageDto(
        Long id,
        Long senderId,
        Long recipientId,
        String content,
        Instant sentAt,
        Instant editedAt,
        boolean deleted,
        boolean read) {

    public static ChatMessageDto from(ChatMessage message) {
        return new ChatMessageDto(
                message.getId(),
                message.getSender().getId(),
                message.getRecipient().getId(),
                message.isDeleted() ? "" : message.getContent(),
                message.getSentAt(),
                message.getEditedAt(),
                message.isDeleted(),
                message.getReadAt() != null);
    }
}
