package com.connect.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void create(Student recipient, Student actor, NotificationType type,
                       Long referenceId, String message) {
        notificationRepository.save(new Notification(recipient, actor, type, referenceId, message));
    }

    /**
     * One aggregated message notification per unread conversation instead of one
     * notification per message. Repeated messages simply update the count.
     */
    @Transactional
    public void recordMessage(Student recipient, Student actor, long unreadCount) {
        String text = unreadCount == 1
                ? actor.getName() + " sent you a message."
                : actor.getName() + " sent you " + unreadCount + " new messages.";
        Optional<Notification> existing = notificationRepository
                .findFirstByRecipientIdAndActorIdAndTypeAndReadAtIsNull(
                        recipient.getId(), actor.getId(), NotificationType.MESSAGE);
        if (existing.isPresent()) {
            Notification notification = existing.get();
            notification.setMessage(text);
            notificationRepository.save(notification);
        } else {
            notificationRepository.save(new Notification(
                    recipient, actor, NotificationType.MESSAGE, actor.getId(), text));
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
                });
    }

    @Transactional(readOnly = true)
    public NotificationInboxDto inbox(Long studentId, int page, int size) {
        Page<Notification> result = notificationRepository
                .findByRecipientIdOrderByCreatedAtDescIdDesc(studentId, PageRequest.of(page, size));
        List<NotificationDto> content = result.getContent().stream().map(NotificationDto::from).toList();
        long unreadCount = notificationRepository.countByRecipientIdAndReadAtIsNull(studentId);
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
        }
    }

    @Transactional
    public void markAllRead(Long studentId) {
        notificationRepository.markAllRead(studentId, Instant.now());
    }
}
