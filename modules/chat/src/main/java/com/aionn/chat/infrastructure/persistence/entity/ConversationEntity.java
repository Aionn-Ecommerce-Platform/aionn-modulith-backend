package com.aionn.chat.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "chat_conversations", indexes = {
        @Index(name = "idx_conversations_buyer", columnList = "buyer_id"),
        @Index(name = "idx_conversations_merchant", columnList = "merchant_id"),
        @Index(name = "idx_conversations_pair", columnList = "buyer_id, merchant_id", unique = true),
        @Index(name = "idx_conversations_last_msg", columnList = "last_message_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationEntity {

    @Id
    @Column(name = "conversation_id", length = 50)
    private String conversationId;

    @Column(name = "buyer_id", length = 50, nullable = false)
    private String buyerId;

    @Column(name = "merchant_id", length = 50, nullable = false)
    private String merchantId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "participants", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> participants;

    @Column(name = "last_message_id", length = 50)
    private String lastMessageId;

    @Column(name = "last_message_preview", columnDefinition = "TEXT")
    private String lastMessagePreview;

    @Column(name = "last_message_type", length = 20)
    private String lastMessageType;

    @Column(name = "last_message_sender_id", length = 50)
    private String lastMessageSenderId;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private long version;
}
