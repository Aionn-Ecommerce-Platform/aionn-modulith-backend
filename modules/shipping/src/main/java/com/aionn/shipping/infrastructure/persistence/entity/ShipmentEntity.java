package com.aionn.shipping.infrastructure.persistence.entity;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shipments", indexes = {
        @Index(name = "idx_shipments_order", columnList = "order_id"),
        @Index(name = "idx_shipments_merchant", columnList = "merchant_id"),
        @Index(name = "idx_shipments_user", columnList = "user_id"),
        @Index(name = "idx_shipments_tracking", columnList = "tracking_code", unique = true),
        @Index(name = "idx_shipments_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentEntity {

    @Id
    @Column(name = "shipment_id", length = 50)
    private String shipmentId;

    @Column(name = "order_id", length = 50, nullable = false)
    private String orderId;

    @Column(name = "merchant_id", length = 50, nullable = false)
    private String merchantId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "tracking_code", length = 100)
    private String trackingCode;

    @Column(name = "carrier_order_id", length = 100)
    private String carrierOrderId;

    @Column(name = "label_url", columnDefinition = "TEXT")
    private String labelUrl;

    @Column(name = "weight_gram", nullable = false)
    private int weightGram;

    @Column(name = "length_cm", precision = 8, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "width_cm", precision = 8, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "height_cm", precision = 8, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "to_full_name", length = 255)
    private String toFullName;

    @Column(name = "to_phone", length = 30)
    private String toPhone;

    @Column(name = "to_address_line", columnDefinition = "TEXT")
    private String toAddressLine;

    @Column(name = "to_ward_code", length = 30)
    private String toWardCode;

    @Column(name = "to_district_id", length = 30)
    private String toDistrictId;

    @Column(name = "to_province_code", length = 30)
    private String toProvinceCode;

    @Column(name = "to_country_code", length = 5)
    private String toCountryCode;

    @Column(name = "cod_amount", precision = 18, scale = 2)
    private BigDecimal codAmount;

    @Column(name = "shipping_fee", precision = 18, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "current_location", length = 255)
    private String currentLocation;

    @Column(name = "shipper_name", length = 255)
    private String shipperName;

    @Column(name = "shipper_phone", length = 30)
    private String shipperPhone;

    @Column(name = "signature_url", columnDefinition = "TEXT")
    private String signatureUrl;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_failure_reason", columnDefinition = "TEXT")
    private String lastFailureReason;

    @Column(name = "issue_type", length = 50)
    private String issueType;

    @Column(name = "issue_resolution", columnDefinition = "TEXT")
    private String issueResolution;

    @Column(name = "expected_delivery_date")
    private Instant expectedDeliveryDate;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "picked_at")
    private Instant pickedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private long version;
}
