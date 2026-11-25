package com.connect.service;

import com.connect.dto.NotificationSocketEvent;

/** Application event delivered to a student's active WebSocket sessions after commit. */
public record NotificationRealtimeEvent(Long recipientId, NotificationSocketEvent payload) {}
