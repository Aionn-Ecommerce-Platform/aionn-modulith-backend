package com.aionn.shipping.application.usecase.shipment;

import com.aionn.shipping.application.dto.shipment.command.FetchLabelCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.application.mapper.ShipmentResultMapper;
import com.aionn.shipping.application.port.in.shipment.FetchLabelInputPort;
import com.aionn.shipping.application.service.ShipmentCarrierOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FetchLabelUseCase implements FetchLabelInputPort {

    private final ShipmentCarrierOrchestrator shipmentCarrierOrchestrator;
    private final ShipmentResultMapper shipmentResultMapper;

    @Override
    public ShipmentResult execute(FetchLabelCommand command) {
        return shipmentResultMapper.toResult(shipmentCarrierOrchestrator.fetchLabel(command));
    }
}
