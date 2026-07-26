package com.aionn.shipping.application.usecase.shipment;

import com.aionn.shipping.application.dto.rate.result.ShippingQuoteResult;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.application.port.in.shipment.QuoteShippingInputPort;
import com.aionn.shipping.application.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuoteShippingUseCase implements QuoteShippingInputPort {

    private final ShipmentService shipmentService;

    @Override
    public ShippingQuoteResult execute(QuoteShippingCommand command) {
        return shipmentService.quote(command);
    }
}
