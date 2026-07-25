package com.aionn.shipping.application.usecase.shipment;

import com.aionn.shipping.application.dto.shipment.command.CancelShipmentCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.application.mapper.ShipmentResultMapper;
import com.aionn.shipping.application.port.in.shipment.CancelShipmentInputPort;
import com.aionn.shipping.application.service.ShipmentCarrierOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CancelShipmentUseCase implements CancelShipmentInputPort {

    private final ShipmentCarrierOrchestrator shipmentCarrierOrchestrator;
    private final ShipmentResultMapper shipmentResultMapper;

    @Override
    public ShipmentResult execute(CancelShipmentCommand command) {
        return shipmentResultMapper.toResult(shipmentCarrierOrchestrator.cancelShipment(command));
    }
}
