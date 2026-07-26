package com.aionn.shipping.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

public final class ShipmentEvents {

    private ShipmentEvents() {
    }

    public record ShipmentRequested(
            String shipmentId,
            String orderId,
            int weightGram,
            String dimensions,
            String toDistrictId,
            String toWardCode,
            Instant occurredAt) implements ShippingEvent {
    }

    public record ShipmentRegistered(
            String shipmentId,
            String orderId,
            String trackingCode,
            String carrierOrderId,
            Instant expectedDate,
            Instant occurredAt) implements ShippingEvent {
    }

    public record ShippingLabelFetched(
            String shipmentId,
            String trackingCode,
            String labelUrl,
            Instant occurredAt) implements ShippingEvent {
    }

    public record ShipmentPickedUp(
            String shipmentId,
            String warehouseId,
            Instant pickedAt,
            Instant occurredAt) implements ShippingEvent {
    }

    public record ShipmentStatusUpdated(
            String shipmentId,
            String status,
            String currentLocation,
            String statusDesc,
            Instant occurredAt) implements ShippingEvent {
    }

    public record ShipmentOutForDelivery(
            String shipmentId,
            String orderId,
            String shipperName,
            String shipperPhone,
            Instant occurredAt) implements ShippingEvent {
    }

    public record ShipmentDelivered(
            String shipmentId,
            String orderId,
            String signatureUrl,
            Instant deliveredAt,
            Instant occurredAt) implements ShippingEvent {
    }

    public record ShipmentDeliveryFailed(
            String shipmentId,
            String reason,
            int attemptCount,
            Instant occurredAt) implements ShippingEvent {
    }

    public record ShipmentReturned(
            String shipmentId,
            String returnReason,
            Instant returnedAt,
            Instant occurredAt) implements ShippingEvent {
    }

    public record ShipmentCancelled(
            String shipmentId,
            String orderId,
            String reason,
            Instant cancelledAt,
            Instant occurredAt) implements ShippingEvent {
    }

    public record ShippingIssueResolved(
            String shipmentId,
            String issueType,
            String resolution,
            Instant resolvedAt,
            Instant occurredAt) implements ShippingEvent {
    }

    public record ShippingRateConfigured(
            String rateId,
            String zoneCode,
            BigDecimal baseFee,
            String currency,
            String condition,
            Instant configuredAt,
            Instant occurredAt) implements ShippingEvent {
    }
}

