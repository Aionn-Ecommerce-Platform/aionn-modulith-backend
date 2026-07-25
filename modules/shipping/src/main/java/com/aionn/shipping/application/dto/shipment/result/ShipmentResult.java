package com.aionn.shipping.application.dto.shipment.result;

import java.math.BigDecimal;
import java.time.Instant;

public record ShipmentResult(
                String shipmentId,
                String orderId,
                String merchantId,
                String userId,
                String trackingCode,
                String carrierOrderId,
                String labelUrl,
                BigDecimal codAmount,
                BigDecimal shippingFee,
                String currency,
                String status,
                String currentLocation,
                String shipperName,
                String shipperPhone,
                int attemptCount,
                String lastFailureReason,
                Instant expectedDeliveryDate,
                Instant pickedAt,
                Instant deliveredAt,
                Instant cancelledAt,
                Instant returnedAt,
                Instant createdAt,
                Instant updatedAt) {
}
