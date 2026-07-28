package com.aionn.chat.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "chat_merchant_auto_replies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantAutoReplyEntity {

    @Id
    @Column(name = "merchant_id", length = 50)
    private String merchantId;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "greeting", columnDefinition = "TEXT")
    private String greeting;

    @Column(name = "away_message", columnDefinition = "TEXT")
    private String awayMessage;

    @Column(name = "working_hour_start")
    private LocalTime workingHourStart;

    @Column(name = "working_hour_end")
    private LocalTime workingHourEnd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "working_days", columnDefinition = "jsonb", nullable = false)
    private List<String> workingDays;

    @Column(name = "timezone", length = 50, nullable = false)
    private String timezone;

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
