package com.connect.dto;

import java.util.List;

public record NotificationInboxDto(List<NotificationDto> content, int page, int size,
                                   long totalElements, int totalPages, long unreadCount) {}
