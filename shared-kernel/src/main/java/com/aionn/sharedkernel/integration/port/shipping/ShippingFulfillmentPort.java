package com.aionn.sharedkernel.integration.port.shipping;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingFulfillmentPort {

        QuoteResult quote(String orderId, String merchantId, Address address, String currency,
                        List<Parcel> parcels, BigDecimal goodsSubtotal);

        /** One shipment per carrier consignment: oversized carts become several trackable parcels. */
        List<RegistrationResult> createAndRegister(
                        String orderId,
                        String merchantId,
                        String userId,
                        Address address,
                        BigDecimal codAmount,
                        BigDecimal shippingFee,
                        String currency,
                        List<Parcel> parcels);

        record Address(
                        String fullName,
                        String phone,
                        String addressLine,
                        String wardCode,
                        String districtCode,
                        String provinceCode,
                        String countryCode) {
        }

        record QuoteResult(BigDecimal fee, String currency) {
        }

        record RegistrationResult(
                        String shipmentId,
                        String trackingCode,
                        String carrierOrderId,
                        String labelUrl) {
        }

        /**
         * Parcel characteristics for one SKU in the shipment. {@code weightGram}
         * is the total weight for {@code qty} units; box dims describe one unit.
         */
        record Parcel(
                        Integer weightGram,
                        java.math.BigDecimal lengthCm,
                        java.math.BigDecimal widthCm,
                        java.math.BigDecimal heightCm,
                        int qty) {
        }
}
