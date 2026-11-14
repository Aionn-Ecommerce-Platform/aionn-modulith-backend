package com.aionn.shipping.application.service;

import java.util.ArrayList;

import com.aionn.shipping.application.dto.rate.result.ShippingQuoteResult;
import com.aionn.shipping.application.dto.shipment.command.CarrierWebhookCommand;
import com.aionn.shipping.application.dto.shipment.command.CreateShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.application.dto.shipment.command.ResolveIssueCommand;
import com.aionn.shipping.application.policy.CarrierWebhookSecurityPolicy;
import com.aionn.shipping.application.policy.ShippingDefaultsPolicy;
import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.application.port.out.CarrierWebhookIdempotencyPort;
import com.aionn.shipping.application.port.out.ShipmentPersistencePort;
import com.aionn.shipping.application.port.out.ShippingRatePersistencePort;
import com.aionn.shipping.application.port.out.integration.ShippingIntegrationEventPublisherPort;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import com.aionn.shipping.domain.valueobject.ShipmentStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.contracts.port.catalog.MerchantQueryPort;
import com.aionn.contracts.port.ordering.OrderQueryPort;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShipmentService {

    private final ShipmentPersistencePort shipmentPersistencePort;
    private final ShippingRatePersistencePort shippingRatePersistencePort;
    private final EventPublisher eventPublisher;
    private final CarrierClient carrierClient;
    private final ShippingIntegrationEventPublisherPort integrationEventPublisher;
    private final MerchantQueryPort merchantQueryPort;
    private final OrderQueryPort orderQueryPort;
    private final ShippingDefaultsPolicy defaultsPolicy;
    private final Clock clock;
    private final CarrierWebhookSecurityPolicy webhookSecurityPolicy;
    private final CarrierWebhookIdempotencyPort webhookIdempotencyPort;
    private final TransactionTemplate transactionTemplate;

    public Shipment createShipment(CreateShipmentCommand command) {
        return createShipmentsPerConsignment(command).getFirst();
    }

    public List<Shipment> createShipmentsPerConsignment(CreateShipmentCommand command) {
        String merchantId = command.merchantId();
        if (merchantId == null || merchantId.isBlank()) {
            merchantId = merchantQueryPort.findMerchantIdByOwnerId(command.ownerId())
                    .orElseThrow(() -> new ShippingException(ShippingErrorCode.SHIPMENT_FORBIDDEN));
        }
        OrderQueryPort.OrderSummary order = null;
        if (command.ownerId() != null && !command.ownerId().isBlank()) {
            order = orderQueryPort.findOrderSummary(command.orderId())
                    .orElseThrow(() -> new ShippingException(ShippingErrorCode.SHIPMENT_NOT_FOUND));
            if (!merchantId.equals(order.merchantId())
                    || (command.userId() != null && !command.userId().equals(order.userId()))) {
                throw new ShippingException(ShippingErrorCode.SHIPMENT_FORBIDDEN,
                        "Order does not belong to the authenticated merchant/user");
            }
        }
        if (command.address() == null) {
            throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                    "Shipment destination address is required");
        }
        List<ParcelConsolidator.Consignment> consignments = ParcelConsolidator.consolidate(command.parcels());
        if (consignments.isEmpty()) {
            throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                    "Cannot create shipment: no parcel dimensions provided");
        }
        List<Shipment> existing = shipmentPersistencePort.findByOrderId(command.orderId()).stream()
                .filter(shipment -> shipment.getStatus() != ShipmentStatus.CANCELLED)
                .toList();
        if (!existing.isEmpty()) {
            // Retries (for example after a failed carrier registration) must not clone
            // parcels.
            if (existing.size() != consignments.size()) {
                throw new ShippingException(ShippingErrorCode.SHIPMENT_INVALID_STATE,
                        "Order already has a different number of active consignments");
            }
            return existing;
        }
        List<Shipment> created = new ArrayList<>(consignments.size());
        String shipmentUserId = order != null ? order.userId() : command.userId();
        BigDecimal codAmount = order != null && order.totalAmount() != null
                ? order.totalAmount()
                : command.codAmount();
        String currency = resolveCurrency(order != null ? order.currency() : command.currency());
        for (ParcelConsolidator.Consignment consignment : consignments) {
            ShipmentDimensions dimensions = new ShipmentDimensions(consignment.weightGram(),
                    consignment.lengthCm(), consignment.widthCm(), consignment.heightCm());
            Shipment shipment = Shipment.request(IdGenerator.ulid(), command.orderId(),
                    merchantId, shipmentUserId, command.address(), dimensions, codAmount,
                    // The quoted fee covers every consignment; keep it on the first row only.
                    created.isEmpty() ? command.shippingFee() : BigDecimal.ZERO,
                    currency, clock);
            Shipment saved = shipmentPersistencePort.save(shipment);
            eventPublisher.publish(shipment.pullEvents());
            created.add(saved);
        }
        return created;
    }

    public Shipment applyRegistration(String shipmentId, CarrierClient.Registration reg) {
        Shipment shipment = required(shipmentId);
        if (shipment.getTrackingCode() != null) {
            return shipment;
        }
        shipment.registerWithCarrier(reg.trackingCode(), reg.carrierOrderId(), reg.expectedDate(), clock);
        Shipment saved = shipmentPersistencePort.save(shipment);
        eventPublisher.publish(shipment.pullEvents());
        return saved;
    }

    public Shipment applyLabel(String shipmentId, String labelUrl) {
        Shipment shipment = required(shipmentId);
        shipment.fetchLabel(labelUrl, clock);
        Shipment saved = shipmentPersistencePort.save(shipment);
        eventPublisher.publish(shipment.pullEvents());
        return saved;
    }

    public Shipment applyCancel(String shipmentId, String reason) {
        Shipment shipment = required(shipmentId);
        shipment.cancel(reason, clock);
        Shipment saved = shipmentPersistencePort.save(shipment);
        eventPublisher.publish(shipment.pullEvents());
        integrationEventPublisher.publishCancelled(saved.getShipmentId(), saved.getOrderId(), reason);
        return saved;
    }

    public Shipment resolveIssue(ResolveIssueCommand command) {
        Shipment shipment = required(command.shipmentId());
        shipment.resolveIssue(command.issueType(), command.resolution(), clock);
        Shipment saved = shipmentPersistencePort.save(shipment);
        eventPublisher.publish(shipment.pullEvents());
        return saved;
    }

    public Shipment applyCarrierWebhook(CarrierWebhookCommand webhook) {
        verifyWebhookSecret(webhook);
        if (!webhookIdempotencyPort.claim(webhook.eventId())) {
            return shipmentPersistencePort.findByTrackingCode(webhook.trackingCode())
                    .orElseThrow(() -> new ShippingException(ShippingErrorCode.SHIPMENT_NOT_FOUND));
        }
        Shipment shipment = shipmentPersistencePort.findByTrackingCode(webhook.trackingCode())
                .orElseThrow(() -> new ShippingException(ShippingErrorCode.SHIPMENT_NOT_FOUND));
        var previousStatus = shipment.getStatus();
        switch (webhook.type()) {
            case "PICKED_UP" -> shipment.markPickedUp(webhook.warehouseId(), clock);
            case "IN_TRANSIT" -> shipment.updateInTransitStatus(webhook.currentLocation(), webhook.statusDesc(), clock);
            case "OUT_FOR_DELIVERY" ->
                shipment.markOutForDelivery(webhook.shipperName(), webhook.shipperPhone(), clock);
            case "DELIVERED" -> shipment.markDelivered(webhook.signatureUrl(), clock);
            case "DELIVERY_FAILED" -> shipment.recordDeliveryFailure(webhook.reason(), clock);
            case "RETURNED" -> shipment.markReturned(webhook.reason(), clock);
            case "RETRY" -> shipment.retryDelivery(clock);
            default -> throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                    "Unknown webhook type: " + webhook.type());
        }
        Shipment saved = shipmentPersistencePort.save(shipment);
        eventPublisher.publish(shipment.pullEvents());
        if (saved.getStatus() != previousStatus) {
            publishIntegration(saved, webhook);
        }
        return saved;
    }

    private void verifyWebhookSecret(CarrierWebhookCommand webhook) {
        boolean authorized = webhook.rawPayload() == null
                ? webhookSecurityPolicy.isAuthorized(webhook.webhookSecret())
                : webhookSecurityPolicy.isAuthorized(webhook.rawPayload(), webhook.signature(),
                        webhook.timestamp(), webhook.webhookSecret());
        if (!authorized) {
            throw new ShippingException(ShippingErrorCode.SHIPMENT_FORBIDDEN,
                    "Invalid webhook signature");
        }
    }

    private void publishIntegration(Shipment saved, CarrierWebhookCommand webhook) {
        switch (webhook.type()) {
            case "PICKED_UP" -> integrationEventPublisher.publishDispatched(
                    saved.getShipmentId(), saved.getOrderId(), saved.getTrackingCode());
            case "DELIVERED" -> {
                boolean late = saved.getExpectedDeliveryDate() != null
                        && saved.getDeliveredAt() != null
                        && saved.getDeliveredAt().isAfter(saved.getExpectedDeliveryDate());
                integrationEventPublisher.publishDelivered(
                        saved.getShipmentId(),
                        saved.getOrderId(),
                        saved.getUserId(),
                        webhook.signatureUrl(),
                        saved.getDeliveredAt(),
                        saved.getExpectedDeliveryDate(),
                        late);
            }
            case "DELIVERY_FAILED" -> integrationEventPublisher.publishDeliveryFailed(
                    saved.getShipmentId(), saved.getOrderId(), saved.getUserId(), webhook.reason(),
                    saved.getAttemptCount());
            case "RETURNED" -> integrationEventPublisher.publishReturned(
                    saved.getShipmentId(), saved.getOrderId(), saved.getUserId(), saved.getMerchantId(),
                    webhook.reason());
            default -> {

            }
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ShippingQuoteResult quote(QuoteShippingCommand command) {
        if (command.address() == null || command.address().provinceCode() == null
                || command.address().provinceCode().isBlank()) {
            throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                    "Cannot quote shipping: destination address is required");
        }
        String currency = resolveCurrency(command.currency());
        List<ParcelConsolidator.Consignment> consignments = ParcelConsolidator.consolidate(command.parcels());
        if (consignments.isEmpty()) {
            throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                    "Cannot quote shipping: no parcel dimensions provided");
        }
        ShippingQuoteResult configuredRate = transactionTemplate.execute(status -> shippingRatePersistencePort
                .findByZoneCode(command.address().provinceCode())
                .map(rate -> {
                    if (!currency.equalsIgnoreCase(rate.getCurrency())) {
                        throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                                "Configured rate currency does not match requested currency");
                    }
                    BigDecimal total = BigDecimal.ZERO;
                    for (ParcelConsolidator.Consignment consignment : consignments) {
                        // Bill the greater of actual and volumetric weight, as the carrier does.
                        int chargeable = new ShipmentDimensions(consignment.weightGram(),
                                consignment.lengthCm(), consignment.widthCm(), consignment.heightCm())
                                .chargeableWeightGram();
                        total = total.add(rate.feeFor(chargeable, command.goodsSubtotal()));
                    }
                    boolean expressRequested = "EXPRESS".equalsIgnoreCase(command.serviceCode());
                    BigDecimal expressTotal = total.add(rate.getExpressExtraFee());
                    Instant expressEta = rate.getExpressEtaDays() == null ? null
                            : clock.instant().plus(Duration.ofDays(rate.getExpressEtaDays()));
                    List<ShippingQuoteResult.ShippingOption> options = new ArrayList<>();
                    options.add(new ShippingQuoteResult.ShippingOption(
                            "STANDARD", "Standard delivery", total, null));
                    if (rate.hasExpress()) {
                        options.add(new ShippingQuoteResult.ShippingOption(
                                "EXPRESS", "Express delivery", expressTotal, expressEta));
                    }
                    BigDecimal selected = expressRequested && rate.hasExpress() ? expressTotal : total;
                    Instant selectedEta = expressRequested && rate.hasExpress() ? expressEta : null;
                    return new ShippingQuoteResult(selected, rate.getCurrency(), rate.getZoneCode(),
                            "configured-rate", rate.getCondition(), selectedEta, null, options);
                })
                .orElse(null));
        if (configuredRate != null) {
            return configuredRate;
        }

        BigDecimal totalFee = BigDecimal.ZERO;
        String zoneCode = null;
        String detail = null;
        Instant expected = null;
        Instant orderDate = null;
        for (ParcelConsolidator.Consignment consignment : consignments) {
            ShipmentDimensions dimensions = new ShipmentDimensions(consignment.weightGram(),
                    consignment.lengthCm(), consignment.widthCm(), consignment.heightCm());
            CarrierClient.Quote q = carrierClient.quote(command.address(), dimensions, currency);
            totalFee = totalFee.add(q.fee() == null ? BigDecimal.ZERO : q.fee());
            zoneCode = q.zoneCode();
            detail = q.detail();
            expected = q.expectedDeliveryDate();
            orderDate = q.orderDate();
        }
        return new ShippingQuoteResult(totalFee, currency, zoneCode, "carrier", detail, expected, orderDate,
                List.of(new ShippingQuoteResult.ShippingOption(
                        "STANDARD", "Standard delivery", totalFee, expected)));
    }

    @Transactional(readOnly = true)
    public Shipment get(String shipmentId, String requesterUserId) {
        Shipment shipment = required(shipmentId);
        ensureViewable(shipment, requesterUserId);
        return shipment;
    }

    @Transactional(readOnly = true)
    public List<Shipment> findByOrderId(String orderId, String requesterUserId) {
        String requesterMerchantId = requesterUserId == null ? null
                : merchantQueryPort.findMerchantIdByOwnerId(requesterUserId).orElse(null);
        return shipmentPersistencePort.findByOrderId(orderId).stream()
                .filter(s -> {
                    try {
                        s.ensureViewableBy(requesterUserId, requesterMerchantId);
                        return true;
                    } catch (ShippingException ex) {
                        return false;
                    }
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Shipment loadShipment(String shipmentId) {
        return required(shipmentId);
    }

    @Transactional(readOnly = true)
    public Shipment loadShipmentForUpdate(String shipmentId) {
        return shipmentPersistencePort.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ShippingException(ShippingErrorCode.SHIPMENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public boolean hasPickedUpShipment(String orderId) {
        return shipmentPersistencePort.findByOrderId(orderId).stream()
                .anyMatch(shipment -> shipment.getStatus().isPickedUp());
    }

    @Transactional(readOnly = true)
    public boolean hasActiveShipment(String orderId) {
        return shipmentPersistencePort.findByOrderId(orderId).stream()
                .anyMatch(shipment -> shipment.getStatus() != ShipmentStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public boolean hasUnregisteredShipment(String orderId) {
        return shipmentPersistencePort.findByOrderId(orderId).stream()
                .anyMatch(shipment -> shipment.getStatus() != ShipmentStatus.CANCELLED
                        && shipment.getTrackingCode() == null);
    }

    @Transactional(readOnly = true)
    public boolean allShipmentsDelivered(String orderId) {
        List<Shipment> active = shipmentPersistencePort.findByOrderId(orderId).stream()
                .filter(shipment -> shipment.getStatus() != ShipmentStatus.CANCELLED)
                .toList();
        return !active.isEmpty()
                && active.stream().allMatch(shipment -> shipment.getStatus() == ShipmentStatus.DELIVERED);
    }

    public void ensureViewable(Shipment shipment, String requesterUserId) {
        String merchantId = requesterUserId == null ? null
                : merchantQueryPort.findMerchantIdByOwnerId(requesterUserId).orElse(null);
        shipment.ensureViewableBy(requesterUserId, merchantId);
    }

    private Shipment required(String shipmentId) {
        return shipmentPersistencePort.findById(shipmentId)
                .orElseThrow(() -> new ShippingException(ShippingErrorCode.SHIPMENT_NOT_FOUND));
    }

    private String resolveCurrency(String currency) {
        return currency == null || currency.isBlank() ? defaultsPolicy.defaultCurrency() : currency;
    }
}
