package com.aionn.ordering.application.port.out;

import com.aionn.ordering.domain.valueobject.ShippingAddress;

import java.math.BigDecimal;

/**
 * Outbound port for shipping quote / dispatch when the order is placed or
 * shipping info is changed.
 */
public interface ShippingGateway {

    ShippingQuote quote(String orderId, String merchantId, ShippingAddress address, String currency);

    Registration createAndRegister(String orderId, String merchantId, String userId,
            ShippingAddress address, BigDecimal codAmount, BigDecimal shippingFee, String currency);

    record ShippingQuote(BigDecimal fee, String currency) {
    }

    record Registration(String shipmentId, String trackingCode, String carrierOrderId, String labelUrl) {
    }
}
