package com.aionn.shipping.application.service;

import com.aionn.shipping.application.dto.shipment.command.CancelShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.FetchLabelCommand;
import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.domain.valueobject.ShipmentStatus;
import com.aionn.contracts.port.catalog.MerchantQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentCarrierOrchestrator {

    private final ShipmentService shipmentService;
    private final CarrierClient carrierClient;
    private final MerchantQueryPort merchantQueryPort;

    @Transactional
    public Shipment registerWithCarrier(String shipmentId, String merchantId) {
        Shipment shipment = shipmentService.loadShipmentForUpdate(shipmentId);

        if (merchantId != null) {
            shipment.ensureOwnedByMerchant(merchantId);
        }
        if (shipment.getTrackingCode() != null) {
            return shipment;
        }
        if (!shipment.getStatus().canTransitionTo(ShipmentStatus.REGISTERED)) {
            throw new ShippingException(ShippingErrorCode.SHIPMENT_INVALID_STATE,
                    "Cannot register shipment in status " + shipment.getStatus());
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
        return cancelWithCarrier(shipment, command.reason());
    }

    public Shipment cancelShipmentSystem(String shipmentId, String reason) {
        Shipment shipment = shipmentService.loadShipment(shipmentId);
        return cancelWithCarrier(shipment, reason);
    }

    private Shipment cancelWithCarrier(Shipment shipment, String reason) {
        if (!shipment.getStatus().canTransitionTo(ShipmentStatus.CANCELLED)) {
            throw new ShippingException(ShippingErrorCode.SHIPMENT_ALREADY_PICKED_UP,
                    "Shipment cannot be cancelled in status " + shipment.getStatus());
        }
        if (shipment.getTrackingCode() != null) {
            try {
                carrierClient.cancel(shipment.getTrackingCode(), reason);
            } catch (RuntimeException ex) {
                throw new ShippingException(ShippingErrorCode.SHIPMENT_CARRIER_ERROR,
                        "Carrier cancellation failed: " + ex.getMessage());
            }
        }
        return shipmentService.applyCancel(shipment.getShipmentId(), reason);
    }
}
