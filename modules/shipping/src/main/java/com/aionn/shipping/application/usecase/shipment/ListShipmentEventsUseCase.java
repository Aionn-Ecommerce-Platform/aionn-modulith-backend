package com.aionn.shipping.application.usecase.shipment;

import com.aionn.shipping.application.dto.shipment.result.ShipmentEventResult;
import com.aionn.shipping.application.mapper.ShipmentResultMapper;
import com.aionn.shipping.application.port.in.shipment.ListShipmentEventsInputPort;
import com.aionn.shipping.application.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListShipmentEventsUseCase implements ListShipmentEventsInputPort {

    private final ShipmentService shipmentService;
    private final ShipmentResultMapper shipmentResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentEventResult> execute(String shipmentId, String requesterUserId) {
        return shipmentService.listEvents(shipmentId, requesterUserId).stream()
                .map(shipmentResultMapper::toEventResult)
                .toList();
    }
}
