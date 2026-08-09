package com.aionn.shipping.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.shipping.domain.event.ShipmentEvents;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import com.aionn.shipping.domain.valueobject.ShipmentStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Getter
public class Shipment extends AggregateRoot {

    private final String shipmentId;
    private final String orderId;
    private final String merchantId;
    private final String userId;
    private final ShipmentAddress address;
    private final ShipmentDimensions dimensions;
    private final BigDecimal codAmount;
    private final BigDecimal shippingFee;
    private final String currency;

    private String trackingCode;
    private String carrierOrderId;
    private String labelUrl;
    private String currentLocation;
    private String shipperName;
    private String shipperPhone;
    private String signatureUrl;
    private int attemptCount;
    private String lastFailureReason;
    private String issueType;
    private String issueResolution;

    private Instant expectedDeliveryDate;
    private ShipmentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant pickedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;
    private Instant returnedAt;

    public Shipment(String shipmentId, String orderId, String merchantId, String userId,
            ShipmentAddress address, ShipmentDimensions dimensions, BigDecimal codAmount,
            BigDecimal shippingFee, String currency, String trackingCode, String carrierOrderId,
            String labelUrl, String currentLocation, String shipperName, String shipperPhone,
            String signatureUrl, int attemptCount, String lastFailureReason, String issueType,
            String issueResolution, Instant expectedDeliveryDate, ShipmentStatus status, Instant createdAt, Instant updatedAt,
            Instant pickedAt, Instant deliveredAt, Instant cancelledAt, Instant returnedAt) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.merchantId = merchantId;
        this.userId = userId;
        this.address = address;
        this.dimensions = dimensions;
        this.codAmount = codAmount;
        this.shippingFee = shippingFee;
        this.currency = currency;
        this.trackingCode = trackingCode;
        this.carrierOrderId = carrierOrderId;
        this.labelUrl = labelUrl;
        this.currentLocation = currentLocation;
        this.shipperName = shipperName;
        this.shipperPhone = shipperPhone;
        this.signatureUrl = signatureUrl;
        this.attemptCount = attemptCount;
        this.lastFailureReason = lastFailureReason;
        this.issueType = issueType;
        this.issueResolution = issueResolution;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.pickedAt = pickedAt;
        this.deliveredAt = deliveredAt;
        this.cancelledAt = cancelledAt;
        this.returnedAt = returnedAt;
    }

    public static Shipment request(
            String shipmentId,
            String orderId,
            String merchantId,
            String userId,
            ShipmentAddress address,
            ShipmentDimensions dimensions,
            BigDecimal codAmount,
            BigDecimal shippingFee,
            String currency,
            Clock clock) {
        Guard.require(orderId != null && !orderId.isBlank(),
                () -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "orderId is required"));
        Guard.require(merchantId != null && !merchantId.isBlank(),
                () -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "merchantId is required"));
        Guard.require(userId != null && !userId.isBlank(),
                () -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "userId is required"));
        Guard.require(address != null,
                () -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "address is required"));
        Guard.require(dimensions != null,
                () -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "dimensions are required"));

        Instant now = clock.instant();
        Shipment s = new Shipment(shipmentId, orderId, merchantId, userId, address, dimensions,
                codAmount, shippingFee, currency,
                null, null, null, null, null, null, null, 0, null, null, null,
                null, ShipmentStatus.REQUESTED, now, now, null, null, null, null);
        s.registerEvent(new ShipmentEvents.ShipmentRequested(shipmentId, orderId,
                dimensions.weightGram(), dimensionsString(dimensions),
                address.districtId(), address.wardCode(), now));
        return s;
    }

    public void ensureOwnedByMerchant(String merchantId) {
        Guard.require(this.merchantId.equals(merchantId),
                () -> new ShippingException(ShippingErrorCode.SHIPMENT_FORBIDDEN));
    }

    public void ensureViewableBy(String userId, String merchantId) {
        boolean isBuyer = userId != null && userId.equals(this.userId);
        boolean isSeller = merchantId != null && merchantId.equals(this.merchantId);
        Guard.require(isBuyer || isSeller,
                () -> new ShippingException(ShippingErrorCode.SHIPMENT_FORBIDDEN));
    }

    public void registerWithCarrier(String trackingCode, String carrierOrderId, Instant expectedDate, Clock clock) {
        ensureTransition(ShipmentStatus.REGISTERED);
        this.trackingCode = trackingCode;
        this.carrierOrderId = carrierOrderId;
        this.expectedDeliveryDate = expectedDate;
        this.status = ShipmentStatus.REGISTERED;
        touch(clock);
        registerEvent(new ShipmentEvents.ShipmentRegistered(shipmentId, orderId, trackingCode, carrierOrderId, expectedDate,
                updatedAt));
    }

    public void fetchLabel(String labelUrl, Clock clock) {
        Guard.require(status == ShipmentStatus.REGISTERED || status == ShipmentStatus.PICKED_UP,
                () -> new ShippingException(ShippingErrorCode.SHIPMENT_INVALID_STATE,
                        "Label only available after carrier registration"));
        this.labelUrl = labelUrl;
        touch(clock);
        registerEvent(new ShipmentEvents.ShippingLabelFetched(shipmentId, trackingCode, labelUrl, updatedAt));
    }

    public void markPickedUp(String warehouseId, Clock clock) {
        if (status == ShipmentStatus.PICKED_UP) {
            return;
        }
        ensureTransition(ShipmentStatus.PICKED_UP);
        Instant now = clock.instant();
        this.status = ShipmentStatus.PICKED_UP;
        this.pickedAt = now;
        this.updatedAt = now;
        registerEvent(new ShipmentEvents.ShipmentPickedUp(shipmentId, warehouseId, pickedAt, now));
    }

    public void updateInTransitStatus(String currentLocation, String statusDesc, Clock clock) {
        if (status == ShipmentStatus.IN_TRANSIT) {
            this.currentLocation = currentLocation;
            touch(clock);
            registerEvent(new ShipmentEvents.ShipmentStatusUpdated(shipmentId, status.name(), currentLocation,
                    statusDesc, updatedAt));
            return;
        }
        ensureTransition(ShipmentStatus.IN_TRANSIT);
        this.currentLocation = currentLocation;
        this.status = ShipmentStatus.IN_TRANSIT;
        touch(clock);
        registerEvent(new ShipmentEvents.ShipmentStatusUpdated(shipmentId, status.name(), currentLocation, statusDesc,
                updatedAt));
    }

    public void markOutForDelivery(String shipperName, String shipperPhone, Clock clock) {
        if (status == ShipmentStatus.OUT_FOR_DELIVERY) {
            return;
        }
        ensureTransition(ShipmentStatus.OUT_FOR_DELIVERY);
        this.shipperName = shipperName;
        this.shipperPhone = shipperPhone;
        this.status = ShipmentStatus.OUT_FOR_DELIVERY;
        touch(clock);
        registerEvent(new ShipmentEvents.ShipmentOutForDelivery(shipmentId, orderId, shipperName, shipperPhone, updatedAt));
    }

    public void markDelivered(String signatureUrl, Clock clock) {
        if (status == ShipmentStatus.DELIVERED) {
            return;
        }
        ensureTransition(ShipmentStatus.DELIVERED);
        Instant now = clock.instant();
        this.signatureUrl = signatureUrl;
        this.status = ShipmentStatus.DELIVERED;
        this.deliveredAt = now;
        this.updatedAt = now;
        registerEvent(new ShipmentEvents.ShipmentDelivered(shipmentId, orderId, signatureUrl, deliveredAt, now));
    }

    public void recordDeliveryFailure(String reason, Clock clock) {
        if (status == ShipmentStatus.DELIVERY_FAILED) {
            return;
        }
        ensureTransition(ShipmentStatus.DELIVERY_FAILED);
        this.attemptCount++;
        this.lastFailureReason = reason;
        this.status = ShipmentStatus.DELIVERY_FAILED;
        touch(clock);
        registerEvent(new ShipmentEvents.ShipmentDeliveryFailed(shipmentId, reason, attemptCount, updatedAt));
    }

    public void retryDelivery(Clock clock) {
        Guard.require(status == ShipmentStatus.DELIVERY_FAILED,
                () -> new ShippingException(ShippingErrorCode.SHIPMENT_INVALID_STATE,
                        "Only failed deliveries can be retried"));
        this.status = ShipmentStatus.OUT_FOR_DELIVERY;
        touch(clock);
        registerEvent(new ShipmentEvents.ShipmentStatusUpdated(shipmentId, status.name(), currentLocation,
                "retry attempt " + attemptCount, updatedAt));
    }

    public void markReturned(String returnReason, Clock clock) {
        if (status == ShipmentStatus.RETURNED) {
            return;
        }
        ensureTransition(ShipmentStatus.RETURNED);
        Instant now = clock.instant();
        this.status = ShipmentStatus.RETURNED;
        this.returnedAt = now;
        this.updatedAt = now;
        registerEvent(new ShipmentEvents.ShipmentReturned(shipmentId, returnReason, returnedAt, now));
    }

    public void cancel(String reason, Clock clock) {
        if (status == ShipmentStatus.CANCELLED) {
            return;
        }
        Guard.require(!status.isPickedUp(),
                () -> new ShippingException(ShippingErrorCode.SHIPMENT_ALREADY_PICKED_UP));
        ensureTransition(ShipmentStatus.CANCELLED);
        Instant now = clock.instant();
        this.status = ShipmentStatus.CANCELLED;
        this.cancelledAt = now;
        this.updatedAt = now;
        registerEvent(new ShipmentEvents.ShipmentCancelled(shipmentId, orderId, reason, cancelledAt, now));
    }

    public boolean isCancellable() {
        return status.canTransitionTo(ShipmentStatus.CANCELLED);
    }

    public void resolveIssue(String issueType, String resolution, Clock clock) {
        this.issueType = issueType;
        this.issueResolution = resolution;
        Instant now = clock.instant();
        this.updatedAt = now;
        registerEvent(new ShipmentEvents.ShippingIssueResolved(shipmentId, issueType, resolution, now, now));
    }

    private static String dimensionsString(ShipmentDimensions d) {
        return d.lengthCm() + "x" + d.widthCm() + "x" + d.heightCm() + " cm, " + d.weightGram() + "g";
    }

    private void ensureTransition(ShipmentStatus next) {
        Guard.require(status.canTransitionTo(next),
                () -> new ShippingException(ShippingErrorCode.SHIPMENT_INVALID_STATE,
                        "Cannot transition shipment from " + status + " to " + next));
    }

    private void touch(Clock clock) {
        this.updatedAt = clock.instant();
    }

    @Override
    protected String aggregateId() {
        return shipmentId;
    }
}
