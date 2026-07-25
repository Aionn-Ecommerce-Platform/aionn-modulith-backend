package com.aionn.shipping.application.usecase.shipment;

import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.application.mapper.ShipmentResultMapper;
import com.aionn.shipping.application.port.in.shipment.ListShipmentsByOrderInputPort;
import com.aionn.shipping.application.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListShipmentsByOrderUseCase implements ListShipmentsByOrderInputPort {

    private final ShipmentService shipmentService;
    private final ShipmentResultMapper shipmentResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResult> execute(String orderId, String requesterUserId) {
        return shipmentService.findByOrderId(orderId, requesterUserId).stream()
                .map(shipmentResultMapper::toResult)
                .toList();
    }
}
