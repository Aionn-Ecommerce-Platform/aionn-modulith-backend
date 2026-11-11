package com.aionn.ordering.infrastructure.integration.order;

import com.aionn.contracts.port.ordering.CheckoutQuotePort;
import com.aionn.ordering.application.port.out.CatalogPricingGateway;
import com.aionn.ordering.application.port.out.ShippingGateway;
import com.aionn.sharedkernel.util.MoneyMinorUnits;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrderingCheckoutQuoteAdapter implements CheckoutQuotePort {

    private final CatalogPricingGateway pricingGateway;
    private final ShippingGateway shippingGateway;

    @Override
    public Quote quote(QuoteRequest request) {
        List<String> skuIds = request.lines().stream().map(Line::skuId).toList();
        Map<String, CatalogPricingGateway.SkuPricing> pricing = pricingGateway.resolve(skuIds);
        if (pricing.isEmpty()) {
            return new Quote(request.currency(), 0L, 0L, 0L, 0L);
        }

        String currency = request.currency();
        String merchantId = null;
        BigDecimal subtotal = BigDecimal.ZERO;
        List<ShippingGateway.Parcel> parcels = new ArrayList<>();
        List<CheckoutQuotePort.LinePrice> linePrices = new ArrayList<>();

        for (Line line : request.lines()) {
            CatalogPricingGateway.SkuPricing sku = pricing.get(line.skuId());
            if (sku == null || !sku.active() || sku.price() == null) {
                return new Quote(currency, 0L, 0L, 0L, 0L);
            }
            if (merchantId == null) {
                merchantId = sku.merchantId();
            } else if (!merchantId.equals(sku.merchantId())) {
                // UCP orders are single-merchant; a mixed quote cannot be priced reliably.
                return new Quote(currency, 0L, 0L, 0L, 0L);
            }
            subtotal = subtotal.add(sku.price().multiply(BigDecimal.valueOf(line.qty())));
            linePrices.add(new CheckoutQuotePort.LinePrice(
                    line.skuId(),
                    MoneyMinorUnits.toMinorUnits(sku.price(), currency),
                    MoneyMinorUnits.toMinorUnits(
                            sku.price().multiply(BigDecimal.valueOf(line.qty())), currency)));
            CatalogPricingGateway.SkuPricing.Parcel parcel = sku.parcel();
            if (parcel != null && parcel.hasWeight()) {
                parcels.add(new ShippingGateway.Parcel(
                        parcel.weightGram() * line.qty(),
                        parcel.lengthCm(),
                        parcel.widthCm(),
                        parcel.heightCm(),
                        line.qty()));
            }
        }

        com.aionn.ordering.domain.valueobject.ShippingAddress address = request.address() == null ? null
                : new com.aionn.ordering.domain.valueobject.ShippingAddress(
                        request.address().addressId(),
                        request.address().contactName(),
                        request.address().phone(),
                        request.address().addressLine(),
                        request.address().wardCode(),
                        request.address().districtCode(),
                        request.address().provinceCode(),
                        request.address().countryCode());

        BigDecimal shipping = BigDecimal.ZERO;
        if (address != null && !parcels.isEmpty()) {
            try {
                shipping = shippingGateway
                        .quote("ucp-quote", merchantId, address, currency, parcels, subtotal)
                        .fee();
            } catch (RuntimeException ex) {
                // Quoting is best-effort; the checkout response still carries the merchandise
                // subtotal and completion returns the authoritative total.
                shipping = BigDecimal.ZERO;
            }
        }

        long subtotalMinor = MoneyMinorUnits.toMinorUnits(subtotal, currency);
        long shippingMinor = MoneyMinorUnits.toMinorUnits(shipping, currency);
        return new Quote(currency, subtotalMinor, shippingMinor, 0L, subtotalMinor + shippingMinor,
                linePrices);
    }
}
