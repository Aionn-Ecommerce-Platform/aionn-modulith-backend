package com.aionn.sharedkernel.integration.port.shipping;

import java.math.BigDecimal;
import java.util.List;

// Outbound port for quoting shipping fees and creating shipments. Both calls are
// synchronous so the order can be priced and the shipment id persisted inline.
public interface ShippingGatewayPort {

        // A quote requires real parcel data; null/empty parcels must not be priced from a guess.
        ShippingQuote quote(String orderId, String merchantId, ShippingAddress address, String currency,
                        List<ParcelLine> parcels);

        RegistrationResult createShipment(
                        String orderId,
                        String merchantId,
                        String userId,
                        ShippingAddress address,
                        BigDecimal codAmount,
                        BigDecimal shippingFee,
                        String currency,
                        List<ParcelLine> parcels);

        // Address snapshot shared by Ordering and Shipping without leaking domain types.
        record ShippingAddress(
                        String addressId,
                        String fullName,
                        String phone,
                        String addressLine,
                        String wardCode,
                        String districtCode,
                        String provinceCode,
                        String countryCode) {
        }

        record ShippingQuote(BigDecimal fee, String currency) {
        }

        record RegistrationResult(
                        String shipmentId,
                        String trackingCode,
                        String carrierOrderId,
                        String labelUrl) {
        }

        /** Weight is for {@code qty} units; box dimensions describe one unit. */
        record ParcelLine(
                        Integer weightGram,
                        java.math.BigDecimal lengthCm,
                        java.math.BigDecimal widthCm,
                        java.math.BigDecimal heightCm,
                        int qty) {
        }
}
