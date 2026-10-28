package com.connect.dto;

import java.time.Instant;

import com.connect.entity.Notification;
import com.connect.entity.NotificationType;

public record NotificationDto(Long id, NotificationType type, StudentBrief actor,
                              Long referenceId, String message, Instant createdAt,
                              Instant readAt) {
    public static NotificationDto from(Notification notification) {
        return new NotificationDto(notification.getId(), notification.getType(),
                notification.getActor() == null ? null : StudentBrief.from(notification.getActor()),
                notification.getReferenceId(), notification.getMessage(),
                notification.getCreatedAt(), notification.getReadAt());
    }
}
