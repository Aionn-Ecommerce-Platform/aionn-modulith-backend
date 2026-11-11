package com.aionn.contracts.port.ordering;

import java.util.List;

/**
 * Price preview for a prospective order. Ordering owns shipping and pricing rules, so UCP checkout
 * asks for a quote instead of duplicating the calculation.
 */
public interface CheckoutQuotePort {

    Quote quote(QuoteRequest request);

    record QuoteRequest(
            String userId,
            List<Line> lines,
            String currency,
            String voucherCode,
            ShippingAddress address) {
    }

    record Line(String skuId, int qty) {
    }

    record ShippingAddress(
            String addressId,
            String contactName,
            String phone,
            String addressLine,
            String wardCode,
            String districtCode,
            String provinceCode,
            String countryCode) {
    }

    /**
     * Amounts are in minor units. {@code discountMinor} is zero until promotion preview support is
     * available; the final order total remains authoritative.
     */
    record Quote(
            String currency,
            long subtotalMinor,
            long shippingMinor,
            long discountMinor,
            long totalMinor,
            List<LinePrice> lines) {

        public Quote(String currency, long subtotalMinor, long shippingMinor,
                long discountMinor, long totalMinor) {
            this(currency, subtotalMinor, shippingMinor, discountMinor, totalMinor, List.of());
        }
    }

    record LinePrice(String skuId, long unitPriceMinor, long lineTotalMinor) {
    }
}
