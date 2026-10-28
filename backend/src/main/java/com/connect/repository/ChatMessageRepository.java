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

    @Query("""
            select m from ChatMessage m
            where (m.sender.id = :a and m.recipient.id = :b)
               or (m.sender.id = :b and m.recipient.id = :a)
            order by m.sentAt desc, m.id desc
            """)
    Page<ChatMessage> findConversation(@Param("a") Long a, @Param("b") Long b, Pageable pageable);

    @Query("""
            select m from ChatMessage m
            where (m.sender.id = :a and m.recipient.id = :b)
               or (m.sender.id = :b and m.recipient.id = :a)
            order by m.sentAt desc, m.id desc
            """)
    List<ChatMessage> findLatest(@Param("a") Long a, @Param("b") Long b, Pageable pageable);

    long countBySenderIdAndRecipientIdAndReadAtIsNull(Long senderId, Long recipientId);

    @Modifying
    @Query("""
            update ChatMessage m set m.readAt = :readAt
            where m.sender.id = :sender and m.recipient.id = :recipient and m.readAt is null
            """)
    int markConversationRead(@Param("sender") Long senderId, @Param("recipient") Long recipientId,
                             @Param("readAt") Instant readAt);
}
