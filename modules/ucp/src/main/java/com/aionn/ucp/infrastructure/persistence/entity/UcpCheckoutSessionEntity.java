package com.aionn.ucp.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * JPA entity persisting a UCP checkout session. Durable counterpart to the
 * in-memory adapter — sessions must survive restarts and be visible across
 * instances so the {@code id} (the protocol's idempotency handle) keeps replaying
 * to the same placed order.
 */
@Entity
@Table(name = "ucp_checkout_sessions", indexes = {
        @Index(name = "idx_ucp_checkout_sessions_cart", columnList = "cart_id"),
        @Index(name = "idx_ucp_checkout_sessions_order", columnList = "order_id"),
        @Index(name = "idx_ucp_checkout_sessions_status", columnList = "status"),
        @Index(name = "idx_ucp_checkout_sessions_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UcpCheckoutSessionEntity {

    @Id
    @Column(name = "id", nullable = false, length = 80)
    private String id;

    @Column(name = "user_id", nullable = false, length = 120)
    private String userId;

    @Column(name = "cart_id", length = 80)
    private String cartId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", columnDefinition = "jsonb")
    private Map<String, Integer> items;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "buyer", columnDefinition = "jsonb")
    private Map<String, Object> buyer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", columnDefinition = "jsonb")
    private Map<String, Object> context;

    @Column(name = "order_id", length = 80)
    private String orderId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "price_snapshot", columnDefinition = "jsonb")
    private Map<String, Long> priceSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payment", columnDefinition = "jsonb")
    private Map<String, Object> payment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fulfillment", columnDefinition = "jsonb")
    private Map<String, Object> fulfillment;

    @Column(name = "voucher_code", length = 80)
    private String voucherCode;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
