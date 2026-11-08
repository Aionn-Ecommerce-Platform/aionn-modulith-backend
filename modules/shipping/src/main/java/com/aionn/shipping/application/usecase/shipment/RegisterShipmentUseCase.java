package com.aionn.shipping.application.usecase.shipment;

import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.application.mapper.ShipmentResultMapper;
import com.aionn.shipping.application.port.in.shipment.RegisterShipmentInputPort;
import com.aionn.shipping.application.service.ShipmentCarrierOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterShipmentUseCase implements RegisterShipmentInputPort {

    private final ShipmentCarrierOrchestrator shipmentCarrierOrchestrator;
    private final ShipmentResultMapper shipmentResultMapper;
    private final com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort merchantQueryPort;

    @Override
    public ShipmentResult execute(String shipmentId, String ownerId) {
        // ownerId is the authenticated user id; resolve it to the merchant they
        // own so the orchestrator can verify shipment ownership. A non-merchant
        // (or unresolvable) ownerId is forbidden, not silently bypassed.
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new com.aionn.shipping.domain.exception.ShippingException(
                        com.aionn.shipping.domain.exception.ShippingErrorCode.SHIPMENT_FORBIDDEN));
        return shipmentResultMapper.toResult(shipmentCarrierOrchestrator.registerWithCarrier(shipmentId, merchantId));
    }

    @Override
    public ShipmentResult executeByMerchant(String shipmentId, String merchantId) {
        return shipmentResultMapper.toResult(shipmentCarrierOrchestrator.registerWithCarrier(shipmentId, merchantId));
    }
}
