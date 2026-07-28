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
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_messages_conv_sent", columnList = "conversation_id, sent_at"),
        @Index(name = "idx_messages_sender", columnList = "sender_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEntity {

    @Id
    @Column(name = "message_id", length = 50)
    private String messageId;

    @Column(name = "conversation_id", length = 50, nullable = false)
    private String conversationId;

    @Column(name = "sender_id", length = 50, nullable = false)
    private String senderId;

    @Column(name = "sender_role", length = 20, nullable = false)
    private String senderRole;

    @Column(name = "type", length = 20, nullable = false)
    private String type;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delivered_to", columnDefinition = "jsonb", nullable = false)
    private Set<String> deliveredTo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "read_by", columnDefinition = "jsonb", nullable = false)
    private Set<String> readBy;

    @Column(name = "is_recalled", nullable = false)
    private boolean recalled;

    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private Instant sentAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private long version;
}
