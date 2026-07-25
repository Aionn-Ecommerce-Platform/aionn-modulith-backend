package com.aionn.shipping.application.usecase.shipment;

import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.application.mapper.ShipmentResultMapper;
import com.aionn.shipping.application.port.in.shipment.GetShipmentInputPort;
import com.aionn.shipping.application.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetShipmentUseCase implements GetShipmentInputPort {

    private final ShipmentService shipmentService;
    private final ShipmentResultMapper shipmentResultMapper;

    @Override
    @Transactional(readOnly = true)
    public ShipmentResult execute(String shipmentId, String requesterUserId) {
        return shipmentResultMapper.toResult(shipmentService.get(shipmentId, requesterUserId));
    }
}
