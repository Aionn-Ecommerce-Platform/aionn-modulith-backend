package com.aionn.contracts.port.shipping;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingFulfillmentPort {

        QuoteResult quote(String orderId, String merchantId, Address address, String currency,
                        List<Parcel> parcels, BigDecimal goodsSubtotal);

        default QuoteResult quote(String orderId, String merchantId, Address address, String currency,
                        List<Parcel> parcels, BigDecimal goodsSubtotal, String serviceCode) {
                return quote(orderId, merchantId, address, currency, parcels, goodsSubtotal);
        }

        List<RegistrationResult> createAndRegister(
                        String orderId,
                        String merchantId,
                        String userId,
                        Address address,
                        BigDecimal codAmount,
                        BigDecimal shippingFee,
                        String currency,
                        List<Parcel> parcels);

        boolean hasPickedUpShipment(String orderId);

        boolean hasActiveShipment(String orderId);

        boolean hasUnregisteredShipment(String orderId);

        boolean allShipmentsDelivered(String orderId);

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
                        String skuId,
                        Integer weightGram,
                        BigDecimal lengthCm,
                        BigDecimal widthCm,
                        BigDecimal heightCm,
                        int qty) {
        }

}
