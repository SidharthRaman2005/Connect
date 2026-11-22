package com.connect.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.connect.entity.Notification;
import com.connect.entity.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("""
            select n from Notification n
            where n.recipient.id = :recipientId
            order by coalesce(n.lastActivityAt, n.createdAt) desc, n.id desc
            """)
    Page<Notification> findInbox(@Param("recipientId") Long recipientId, Pageable pageable);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    Optional<Notification> findFirstByRecipientIdAndActorIdAndTypeAndReadAtIsNull(
            Long recipientId, Long actorId, NotificationType type);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    void deleteByRecipientId(Long recipientId);

    @Modifying
    @Query("update Notification n set n.readAt = :readAt where n.recipient.id = :recipientId and n.readAt is null")
    int markAllRead(@Param("recipientId") Long recipientId, @Param("readAt") Instant readAt);
}
