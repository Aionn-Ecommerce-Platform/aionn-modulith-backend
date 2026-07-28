package com.aionn.chat.infrastructure.persistence.repository;

import com.aionn.chat.infrastructure.persistence.entity.MessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, String> {

    @Query("""
            SELECT m FROM MessageEntity m
              WHERE m.conversationId = :conversationId AND m.sentAt < :before
            ORDER BY m.sentAt DESC
            """)
    List<MessageEntity> findByConversationBefore(String conversationId, Instant before, Pageable pageable);

    @Query("""
            SELECT m FROM MessageEntity m
              WHERE m.conversationId = :conversationId
            ORDER BY m.sentAt DESC
            """)
    List<MessageEntity> findByConversationLatest(String conversationId, Pageable pageable);

    @Query("""
            SELECT COUNT(m) FROM MessageEntity m
              WHERE m.conversationId = :conversationId
                AND m.senderId <> :userId
                AND m.sentAt > :afterInstant
                AND m.recalled = FALSE
            """)
    long countUnread(String conversationId, String userId, Instant afterInstant);
}

