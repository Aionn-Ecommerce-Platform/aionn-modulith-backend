package com.aionn.ordering.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user", columnList = "user_id"),
        @Index(name = "idx_orders_merchant", columnList = "merchant_id"),
        @Index(name = "idx_orders_status_created", columnList = "status, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {

    @Id
    @Column(name = "order_id", length = 50)
    private String orderId;

    @Column(name = "parent_order_id", length = 50)
    private String parentOrderId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "merchant_id", length = 50, nullable = false)
    private String merchantId;

    @Column(name = "proposal_id", length = 50)
    private String proposalId;

    @Column(name = "payment_method_id", length = 50)
    private String paymentMethodId;

    @Column(name = "payment_id", length = 50)
    private String paymentId;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "shipping_fee", precision = 18, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "address_id", length = 50)
    private String addressId;

    @Column(name = "address_full_name", length = 255)
    private String addressFullName;

    @Column(name = "address_phone", length = 30)
    private String addressPhone;

    @Column(name = "address_line", columnDefinition = "TEXT")
    private String addressLine;

    @Column(name = "address_ward_code", length = 30)
    private String addressWardCode;

    @Column(name = "address_district_code", length = 30)
    private String addressDistrictCode;

    @Column(name = "address_province_code", length = 30)
    private String addressProvinceCode;

    @Column(name = "address_country_code", length = 5)
    private String addressCountryCode;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "reason_code", length = 50)
    private String reasonCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id.skuId asc")
    @Builder.Default
    private List<OrderItemEntity> items = new ArrayList<>();

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private long version;
}
