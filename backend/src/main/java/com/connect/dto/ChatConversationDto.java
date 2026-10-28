package com.connect.dto;

import java.time.Instant;

public record ChatConversationDto(StudentBrief other, String lastMessage, Instant lastMessageAt,
                                  long unreadCount) {}
