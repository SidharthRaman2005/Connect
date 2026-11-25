package com.connect.dto;

import java.time.Instant;

/** Realtime envelope sent over the authenticated chat socket when an inbox changes. */
public record NotificationSocketEvent(String type, NotificationDto data, Instant at) {
    public static NotificationSocketEvent created(NotificationDto notification) {
        return new NotificationSocketEvent("notification", notification, Instant.now());
    }

    public static NotificationSocketEvent changed() {
        return new NotificationSocketEvent("notification_changed", null, Instant.now());
    }
}
