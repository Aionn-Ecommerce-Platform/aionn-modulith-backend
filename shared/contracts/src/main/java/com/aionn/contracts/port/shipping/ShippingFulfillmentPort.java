package com.aionn.contracts.port.shipping;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingFulfillmentPort {

        QuoteResult quote(String orderId, String merchantId, Address address, String currency,
                        List<Parcel> parcels, BigDecimal goodsSubtotal);

        /** Quote for a specific service level (STANDARD / EXPRESS). */
        default QuoteResult quote(String orderId, String merchantId, Address address, String currency,
                        List<Parcel> parcels, BigDecimal goodsSubtotal, String serviceCode) {
                return quote(orderId, merchantId, address, currency, parcels, goodsSubtotal);
        }

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

        record Parcel(
                        Integer weightGram,
                        BigDecimal lengthCm,
                        BigDecimal widthCm,
                        BigDecimal heightCm,
                        int qty) {
        }
}
