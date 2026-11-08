package com.aionn.shipping.application.usecase.shipment;

import com.aionn.shipping.application.dto.rate.result.ShippingQuoteResult;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.application.port.in.shipment.QuoteShippingInputPort;
import com.aionn.shipping.application.service.ParcelResolver;
import com.aionn.shipping.application.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuoteShippingUseCase implements QuoteShippingInputPort {

    private final ShipmentService shipmentService;
    private final ParcelResolver parcelResolver;

    @Override
    public ShippingQuoteResult execute(QuoteShippingCommand command) {
        return shipmentService.quote(command);
    }

    @Override
    public ShippingQuoteResult executeForItems(
            String orderId,
            com.aionn.shipping.domain.valueobject.ShipmentAddress address,
            List<ItemRequest> items,
            java.math.BigDecimal goodsSubtotal,
            String currency) {
        List<ParcelResolver.Item> resolverItems = items.stream()
                .map(i -> new ParcelResolver.Item(i.skuId(), i.qty()))
                .toList();
        List<QuoteShippingCommand.Parcel> parcels = parcelResolver.resolveParcels(resolverItems);
        return shipmentService.quote(new QuoteShippingCommand(orderId, address, parcels, goodsSubtotal, currency));
    }

    public record ItemRequest(String skuId, int qty) {
    }
}
