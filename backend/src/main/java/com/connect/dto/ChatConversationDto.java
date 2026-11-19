package com.connect.dto;

import java.time.Instant;

/** A conversation summary, relative to the viewer. */
public record ChatConversationDto(
        StudentBrief other,
        String lastMessage,
        Instant lastMessageAt,
        long unreadCount,
        boolean muted,
        boolean archived) {}
