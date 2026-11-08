package com.aionn.shipping.application.port.in.shipment;

import com.aionn.shipping.application.dto.rate.result.ShippingQuoteResult;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.application.usecase.shipment.QuoteShippingUseCase;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;

import java.util.List;

public interface QuoteShippingInputPort {
    ShippingQuoteResult execute(QuoteShippingCommand command);

    /**
     * Quote for a set of SKUs — parcel dims are resolved server-side from the
     * catalog so the fee comes from real item weights, never a guessed parcel.
     */
    ShippingQuoteResult executeForItems(String orderId, ShipmentAddress address,
            List<QuoteShippingUseCase.ItemRequest> items, java.math.BigDecimal goodsSubtotal, String currency);
}
