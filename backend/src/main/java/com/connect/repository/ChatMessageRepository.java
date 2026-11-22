package com.connect.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.connect.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Two variants for each query: PostgreSQL cannot infer the type of a null
    // `:after` bind parameter, so we never send it as null.

    @Query("""
            select m from ChatMessage m
            where (m.sender.id = :a and m.recipient.id = :b)
               or (m.sender.id = :b and m.recipient.id = :a)
            order by m.sentAt desc, m.id desc
            """)
    Page<ChatMessage> findConversation(@Param("a") Long a, @Param("b") Long b, Pageable pageable);

    @Query("""
            select m from ChatMessage m
            where ((m.sender.id = :a and m.recipient.id = :b)
               or (m.sender.id = :b and m.recipient.id = :a))
              and m.sentAt > :after
            order by m.sentAt desc, m.id desc
            """)
    Page<ChatMessage> findConversationAfter(@Param("a") Long a, @Param("b") Long b,
                                            @Param("after") Instant after, Pageable pageable);

    Optional<ChatMessage> findByIdAndSenderId(Long id, Long senderId);

    /** Messages that arrived while the recipient was offline and are still undelivered. */
    List<ChatMessage> findByRecipientIdAndDeliveredAtIsNullAndReadAtIsNull(Long recipientId);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    long countBySenderIdAndRecipientIdAndReadAtIsNull(Long senderId, Long recipientId);

    @Query("""
            select count(m) from ChatMessage m
            where m.sender.id = :sender and m.recipient.id = :recipient
              and m.readAt is null and m.sentAt > :after
            """)
    long countUnreadAfter(@Param("sender") Long senderId, @Param("recipient") Long recipientId,
                          @Param("after") Instant after);

    @Modifying
    @Query("""
            update ChatMessage m set m.readAt = :readAt
            where m.sender.id = :sender and m.recipient.id = :recipient and m.readAt is null
            """)
    int markConversationRead(@Param("sender") Long senderId, @Param("recipient") Long recipientId,
                             @Param("readAt") Instant readAt);
}
