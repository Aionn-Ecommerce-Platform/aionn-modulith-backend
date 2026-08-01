package com.aionn.shipping.application.service;

import com.aionn.shipping.application.dto.rate.result.ShippingQuoteResult;
import com.aionn.shipping.application.dto.shipment.command.CarrierWebhookCommand;
import com.aionn.shipping.application.dto.shipment.command.CreateShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.application.dto.shipment.command.ResolveIssueCommand;
import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.application.port.out.ShipmentPersistencePort;
import com.aionn.shipping.application.port.out.ShippingRatePersistencePort;
import com.aionn.shipping.application.port.out.integration.ShippingIntegrationEventPublisherPort;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.infrastructure.carrier.config.GhnProperties;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
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
    private final GhnProperties ghnProperties;
    private final TransactionTemplate transactionTemplate;

    public Shipment createShipment(CreateShipmentCommand command) {
        String merchantId = command.merchantId();
        if (merchantId == null || merchantId.isBlank()) {
            merchantId = merchantQueryPort.findMerchantIdByOwnerId(command.ownerId())
                    .orElseThrow(() -> new ShippingException(ShippingErrorCode.SHIPMENT_FORBIDDEN));
        }
        Shipment shipment = Shipment.request(IdGenerator.ulid(), command.orderId(),
                merchantId, command.userId(),
                command.address(), command.dimensions(), command.codAmount(),
                command.shippingFee(), command.currency(), clock);
        Shipment saved = shipmentRepository.save(shipment);
        eventPublisher.publish(shipment.pullEvents());
        return saved;
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
        String expected = ghnProperties.webhookSecret();
        if (expected == null || expected.isBlank()) {
            return;
        }
        if (provided == null || !expected.equals(provided)) {
            throw new ShippingException(ShippingErrorCode.SHIPMENT_FORBIDDEN,
                    "Invalid webhook secret");
        }
    }

    private void publishIntegration(Shipment saved, CarrierWebhookCommand webhook) {
        switch (webhook.type()) {
            case "PICKED_UP" -> integrationEventPublisher.publishDispatched(
                    saved.getShipmentId(), saved.getOrderId(), saved.getTrackingCode());
            case "DELIVERED" -> integrationEventPublisher.publishDelivered(
                    saved.getShipmentId(), saved.getOrderId(), webhook.signatureUrl(), saved.getDeliveredAt());
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
        var rate = transactionTemplate.execute(status -> rateRepository.findByZoneCode(
                command.address().provinceCode()));
        if (rate.isPresent()) {
            return new ShippingQuoteResult(rate.get().getBaseFee(), rate.get().getCurrency(),
                    rate.get().getZoneCode(), "configured-rate", rate.get().getCondition(), null, null);
        }
        CarrierClient.Quote q = carrierClient.quote(command.address(), command.dimensions(), currency);
        return new ShippingQuoteResult(q.fee(), q.currency(), q.zoneCode(), "carrier", q.detail(),
                q.expectedDeliveryDate(), q.orderDate());
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
