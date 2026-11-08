package com.aionn.shipping.application.service;

import com.aionn.shipping.application.dto.rate.result.ShippingQuoteResult;
import com.aionn.shipping.application.dto.shipment.command.CarrierWebhookCommand;
import com.aionn.shipping.application.dto.shipment.command.CreateShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.application.dto.shipment.command.ResolveIssueCommand;
import com.aionn.shipping.application.policy.CarrierWebhookSecurityPolicy;
import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.application.port.out.ShipmentPersistencePort;
import com.aionn.shipping.application.port.out.ShippingRatePersistencePort;
import com.aionn.shipping.application.port.out.integration.ShippingIntegrationEventPublisherPort;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShipmentService {

    private final ShipmentPersistencePort shipmentRepository;
    private final ShippingRatePersistencePort rateRepository;
    private final EventPublisher eventPublisher;
    private final CarrierClient carrierClient;
    private final ShippingIntegrationEventPublisherPort integrationEventPublisher;
    private final MerchantQueryPort merchantQueryPort;
    private final Clock clock;
    private final CarrierWebhookSecurityPolicy webhookSecurityPolicy;
    private final TransactionTemplate transactionTemplate;

    public Shipment createShipment(CreateShipmentCommand command) {
        String merchantId = command.merchantId();
        if (merchantId == null || merchantId.isBlank()) {
            merchantId = merchantQueryPort.findMerchantIdByOwnerId(command.ownerId())
                    .orElseThrow(() -> new ShippingException(ShippingErrorCode.SHIPMENT_FORBIDDEN));
        }
        Shipment shipment = Shipment.request(IdGenerator.ulid(), command.orderId(),
                merchantId, command.userId(),
                command.address(), aggregateParcels(command.parcels()), command.codAmount(),
                command.shippingFee(), command.currency(), clock);
        Shipment saved = shipmentRepository.save(shipment);
        eventPublisher.publish(shipment.pullEvents());
        return saved;
    }

    /**
     * One shipment row per carrier consignment. The order keeps a single shipping fee,
     * while each parcel carries its own tracking code/label.
     */
    public List<Shipment> createShipmentsPerConsignment(CreateShipmentCommand command) {
        String merchantId = command.merchantId();
        if (merchantId == null || merchantId.isBlank()) {
            merchantId = merchantQueryPort.findMerchantIdByOwnerId(command.ownerId())
                    .orElseThrow(() -> new ShippingException(ShippingErrorCode.SHIPMENT_FORBIDDEN));
        }
        List<ParcelConsolidator.Consignment> consignments = ParcelConsolidator.consolidate(command.parcels());
        if (consignments.isEmpty()) {
            throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                    "Cannot create shipment: no parcel dimensions provided");
        }
        List<Shipment> created = new java.util.ArrayList<>(consignments.size());
        for (ParcelConsolidator.Consignment consignment : consignments) {
            ShipmentDimensions dimensions = new ShipmentDimensions(consignment.weightGram(),
                    consignment.lengthCm(), consignment.widthCm(), consignment.heightCm());
            Shipment shipment = Shipment.request(IdGenerator.ulid(), command.orderId(),
                    merchantId, command.userId(), command.address(), dimensions, command.codAmount(),
                    // The quoted fee covers every consignment; keep it on the first row only.
                    created.isEmpty() ? command.shippingFee() : BigDecimal.ZERO,
                    command.currency(), clock);
            Shipment saved = shipmentRepository.save(shipment);
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
        Shipment saved = shipmentRepository.save(shipment);
        eventPublisher.publish(shipment.pullEvents());
        return saved;
    }

    public Shipment applyLabel(String shipmentId, String labelUrl) {
        Shipment shipment = required(shipmentId);
        shipment.fetchLabel(labelUrl, clock);
        Shipment saved = shipmentRepository.save(shipment);
        eventPublisher.publish(shipment.pullEvents());
        return saved;
    }

    public Shipment applyCancel(String shipmentId, String reason) {
        Shipment shipment = required(shipmentId);
        shipment.cancel(reason, clock);
        Shipment saved = shipmentRepository.save(shipment);
        eventPublisher.publish(shipment.pullEvents());
        return saved;
    }

    public Shipment resolveIssue(ResolveIssueCommand command) {
        Shipment shipment = required(command.shipmentId());
        shipment.resolveIssue(command.issueType(), command.resolution(), clock);
        Shipment saved = shipmentRepository.save(shipment);
        eventPublisher.publish(shipment.pullEvents());
        return saved;
    }

    public Shipment applyCarrierWebhook(CarrierWebhookCommand webhook) {
        verifyWebhookSecret(webhook.webhookSecret());
        Shipment shipment = shipmentRepository.findByTrackingCode(webhook.trackingCode())
                .orElseThrow(() -> new ShippingException(ShippingErrorCode.SHIPMENT_NOT_FOUND));
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
        Shipment saved = shipmentRepository.save(shipment);
        eventPublisher.publish(shipment.pullEvents());
        publishIntegration(saved, webhook);
        return saved;
    }

    private void verifyWebhookSecret(String provided) {
        if (!webhookSecurityPolicy.isAuthorized(provided)) {
            throw new ShippingException(ShippingErrorCode.SHIPMENT_FORBIDDEN,
                    "Invalid webhook secret");
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
                    saved.getShipmentId(), saved.getOrderId(), webhook.reason(), saved.getAttemptCount());
            default -> {
                /* in-transit / out-for-delivery / returned do not surface to other contexts */
            }
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ShippingQuoteResult quote(QuoteShippingCommand command) {
        String currency = command.currency() == null ? "VND" : command.currency();
        // Heavy carts ship as several consignments; quote each and add the fees.
        List<ParcelConsolidator.Consignment> consignments = ParcelConsolidator.consolidate(command.parcels());
        if (consignments.isEmpty()) {
            throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                    "Cannot quote shipping: no parcel dimensions provided");
        }
        ShippingQuoteResult configuredRate = transactionTemplate.execute(status -> rateRepository
                .findByZoneCode(command.address().provinceCode())
                .map(rate -> {
                    BigDecimal total = BigDecimal.ZERO;
                    for (ParcelConsolidator.Consignment consignment : consignments) {
                        // Bill the greater of actual and volumetric weight, as the carrier does.
                        int chargeable = new ShipmentDimensions(consignment.weightGram(),
                                consignment.lengthCm(), consignment.widthCm(), consignment.heightCm())
                                .chargeableWeightGram();
                        total = total.add(rate.feeFor(chargeable, command.goodsSubtotal()));
                    }
                    return new ShippingQuoteResult(total, rate.getCurrency(), rate.getZoneCode(),
                            "configured-rate", rate.getCondition(), null, null);
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
        return new ShippingQuoteResult(totalFee, currency, zoneCode, "carrier", detail, expected, orderDate);
    }

    /**
     * Collapses per-SKU parcels into one carrier parcel: summed weight and max box side.
     * Missing or invalid data rejects the quote instead of pricing a guess.
     */
    private ShipmentDimensions aggregateParcels(List<QuoteShippingCommand.Parcel> parcels) {
        if (parcels == null || parcels.isEmpty()) {
            throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                    "Cannot quote shipping: no parcel dimensions provided");
        }
        int totalWeightGram = 0;
        BigDecimal maxLength = BigDecimal.ZERO;
        BigDecimal maxWidth = BigDecimal.ZERO;
        BigDecimal maxHeight = BigDecimal.ZERO;
        for (QuoteShippingCommand.Parcel p : parcels) {
            if (p == null || p.weightGram() == null || p.weightGram() <= 0 || p.qty() <= 0) {
                throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                        "Cannot quote shipping: a SKU is missing its parcel weight");
            }
            totalWeightGram += p.weightGram();
            maxLength = maxLength.max(p.lengthCm() == null ? BigDecimal.ZERO : p.lengthCm());
            maxWidth = maxWidth.max(p.widthCm() == null ? BigDecimal.ZERO : p.widthCm());
            maxHeight = maxHeight.max(p.heightCm() == null ? BigDecimal.ZERO : p.heightCm());
        }
        return new ShipmentDimensions(totalWeightGram, maxLength, maxWidth, maxHeight);
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
        return shipmentRepository.findByOrderId(orderId).stream()
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

    public void ensureViewable(Shipment shipment, String requesterUserId) {
        String merchantId = requesterUserId == null ? null
                : merchantQueryPort.findMerchantIdByOwnerId(requesterUserId).orElse(null);
        shipment.ensureViewableBy(requesterUserId, merchantId);
    }

    private Shipment required(String shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShippingException(ShippingErrorCode.SHIPMENT_NOT_FOUND));
    }
}
