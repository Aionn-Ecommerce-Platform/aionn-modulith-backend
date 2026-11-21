package com.aionn.ordering.application.port.out;

import com.aionn.ordering.domain.valueobject.ShippingAddress;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingGateway {

        ShippingQuote quote(String orderId, String merchantId, ShippingAddress address, String currency,
                        List<Parcel> parcels, BigDecimal goodsSubtotal);

        default ShippingQuote quote(String orderId, String merchantId, ShippingAddress address, String currency,
                        List<Parcel> parcels, BigDecimal goodsSubtotal, String serviceCode) {
                return quote(orderId, merchantId, address, currency, parcels, goodsSubtotal);
        }

        List<Registration> createAndRegister(String orderId, String merchantId, String userId,
                        ShippingAddress address, BigDecimal codAmount, BigDecimal shippingFee, String currency,
                        List<Parcel> parcels);

        boolean hasPickedUpShipment(String orderId);

        boolean hasActiveShipment(String orderId);

        boolean hasUnregisteredShipment(String orderId);

        boolean allShipmentsDelivered(String orderId);

        record ShippingQuote(BigDecimal fee, String currency) {
        }

        record Registration(String shipmentId, String trackingCode, String carrierOrderId, String labelUrl) {
        }

        record Parcel(String skuId, Integer weightGram, BigDecimal lengthCm, BigDecimal widthCm,
                        BigDecimal heightCm, int qty) {
        }
}
