package com.connect.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connect.dto.NotificationDto;
import com.connect.dto.NotificationInboxDto;
import com.connect.entity.Notification;
import com.connect.entity.NotificationType;
import com.connect.entity.Student;
import com.connect.exception.ResourceNotFoundException;
import com.connect.repository.NotificationRepository;

/** Persists event notifications and read state for their recipient. */
@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher events;
    private final UserSafetyService safetyService;

    public NotificationService(NotificationRepository notificationRepository, ApplicationEventPublisher events,
                               UserSafetyService safetyService) {
        this.notificationRepository = notificationRepository;
        this.events = events;
        this.safetyService = safetyService;
    }

    public void create(Student recipient, Student actor, NotificationType type,
                       Long referenceId, String message) {
        if (actor != null && safetyService.isBlockedEitherWay(recipient.getId(), actor.getId())) return;
        Notification saved = notificationRepository.save(new Notification(recipient, actor, type, referenceId, message));
        publishCreated(saved);
    }

    /**
     * One plain notification per unread conversation — no message text and no
     * message count. A new message just bumps the activity time so the entry
     * moves back to the top of the inbox.
     */
    @Transactional
    public void recordMessage(Student recipient, Student actor) {
        if (safetyService.isBlockedEitherWay(recipient.getId(), actor.getId())) return;
        String text = actor.getName() + " sent you a message.";
        Optional<Notification> existing = notificationRepository
                .findFirstByRecipientIdAndActorIdAndTypeAndReadAtIsNull(
                        recipient.getId(), actor.getId(), NotificationType.MESSAGE);
        if (existing.isPresent()) {
            Notification notification = existing.get();
            notification.setMessage(text);
            notification.touch();
            publishCreated(notificationRepository.save(notification));
        } else {
            publishCreated(notificationRepository.save(new Notification(
                    recipient, actor, NotificationType.MESSAGE, actor.getId(), text)));
        }
    }

    /** Marks the aggregated message notification for a conversation as read. */
    @Transactional
    public void markMessageNotificationRead(Long recipientId, Long actorId) {
        notificationRepository.findFirstByRecipientIdAndActorIdAndTypeAndReadAtIsNull(
                        recipientId, actorId, NotificationType.MESSAGE)
                .ifPresent(notification -> {
                    notification.setReadAt(Instant.now());
                    notificationRepository.save(notification);
                    publishChanged(recipientId);
                });
    }

    @Transactional(readOnly = true)
    public NotificationInboxDto inbox(Long studentId, int page, int size) {
        List<Long> excludedActors = safetyService.blockedIds(studentId);
        if (excludedActors.isEmpty()) excludedActors = List.of(-1L);
        Page<Notification> result = notificationRepository
                .findInboxExcludingActors(studentId, excludedActors, PageRequest.of(page, size));
        List<NotificationDto> content = result.getContent().stream().map(NotificationDto::from).toList();
        long unreadCount = notificationRepository.countUnreadExcludingActors(studentId, excludedActors);
        return new NotificationInboxDto(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), unreadCount);
    }

    @Transactional
    public void markRead(Long studentId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
            publishChanged(studentId);
        }
    }

    @Transactional
    public void markAllRead(Long studentId) {
        notificationRepository.markAllRead(studentId, Instant.now());
        publishChanged(studentId);
    }

    /** Removes a notification the recipient has just seen/read. */
    @Transactional
    public void deleteNotification(Long studentId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
        notificationRepository.delete(notification);
        publishChanged(studentId);
    }

    /** Clears the whole inbox after the recipient marks everything as read. */
    @Transactional
    public void deleteAllForRecipient(Long studentId) {
        notificationRepository.deleteByRecipientId(studentId);
        publishChanged(studentId);
    }

    private void publishCreated(Notification notification) {
        events.publishEvent(new NotificationRealtimeEvent(notification.getRecipient().getId(),
                com.connect.dto.NotificationSocketEvent.created(NotificationDto.from(notification))));
    }

    private void publishChanged(Long recipientId) {
        events.publishEvent(new NotificationRealtimeEvent(recipientId,
                com.connect.dto.NotificationSocketEvent.changed()));
    }
}
