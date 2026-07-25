package com.aionn.shipping.application.port.in.shipment;

import com.aionn.shipping.application.dto.rate.result.ShippingQuoteResult;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;

public interface QuoteShippingInputPort {
    ShippingQuoteResult execute(QuoteShippingCommand command);
}
