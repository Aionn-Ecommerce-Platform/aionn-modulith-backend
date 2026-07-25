package com.aionn.shipping.application.service;

import com.aionn.shipping.application.dto.shipment.command.CancelShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.FetchLabelCommand;
import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentCarrierOrchestrator {

    private final ShipmentService shipmentService;
    private final CarrierClient carrierClient;
    private final MerchantQueryPort merchantQueryPort;

    public Shipment registerWithCarrier(String shipmentId) {
        Shipment shipment = shipmentService.loadShipment(shipmentId);
        if (shipment.getTrackingCode() != null) {
            return shipment;
        }
        CarrierClient.Registration reg;
        try {
            reg = carrierClient.register(
                    shipment.getShipmentId(),
                    shipment.getOrderId(),
                    shipment.getAddress(),
                    shipment.getDimensions(),
                    shipment.getCodAmount(),
                    shipment.getShippingFee(),
                    shipment.getCurrency());
        } catch (ShippingException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ShippingException(ShippingErrorCode.SHIPMENT_CARRIER_ERROR, ex.getMessage());
        }
        return shipmentService.applyRegistration(shipmentId, reg);
    }

    public Shipment fetchLabel(FetchLabelCommand command) {
        Shipment shipment = shipmentService.loadShipment(command.shipmentId());
        shipmentService.ensureViewable(shipment, command.ownerId());
        if (shipment.getTrackingCode() == null) {
            throw new ShippingException(ShippingErrorCode.SHIPMENT_INVALID_STATE,
                    "Cannot fetch label before carrier registration");
        }
        String labelUrl = carrierClient.fetchLabel(shipment.getTrackingCode());
        return shipmentService.applyLabel(shipment.getShipmentId(), labelUrl);
    }

    public Shipment cancelShipment(CancelShipmentCommand command) {
        Shipment shipment = shipmentService.loadShipment(command.shipmentId());
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(command.ownerId())
                .orElseThrow(() -> new ShippingException(ShippingErrorCode.SHIPMENT_FORBIDDEN));
        shipment.ensureOwnedByMerchant(merchantId);
        if (shipment.getTrackingCode() != null) {
            try {
                carrierClient.cancel(shipment.getTrackingCode(), command.reason());
            } catch (RuntimeException ex) {
                log.warn("Carrier cancel failed for {}: {}", shipment.getTrackingCode(), ex.getMessage());
            }
        }
        return shipmentService.applyCancel(command.shipmentId(), command.reason());
    }
}
