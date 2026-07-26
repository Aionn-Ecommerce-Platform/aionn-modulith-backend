package com.aionn.shipping.application.usecase.shipment;

import com.aionn.shipping.application.dto.shipment.command.CreateShipmentCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.application.mapper.ShipmentResultMapper;
import com.aionn.shipping.application.port.in.shipment.CreateShipmentInputPort;
import com.aionn.shipping.application.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateShipmentUseCase implements CreateShipmentInputPort {

    private final ShipmentService shipmentService;
    private final ShipmentResultMapper shipmentResultMapper;

    @Override
    @Transactional
    public ShipmentResult execute(CreateShipmentCommand command) {
        return shipmentResultMapper.toResult(shipmentService.createShipment(command));
    }
}
