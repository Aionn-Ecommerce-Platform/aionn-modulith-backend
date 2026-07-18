package com.aionn.ordering.infrastructure.integration;

import com.aionn.ordering.application.port.out.ShippingGateway;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.integration.port.shipping.ShippingFulfillmentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ShippingFulfillmentAdapter implements ShippingGateway {

    private final ShippingFulfillmentPort shippingFulfillmentPort;

    @Override
    public ShippingQuote quote(String orderId, String merchantId, ShippingAddress address, String currency) {
        ShippingFulfillmentPort.QuoteResult result = shippingFulfillmentPort.quote(
                orderId, merchantId, toPortAddress(address), currency);
        return new ShippingQuote(result.fee(), result.currency());
    }

    @Override
    public Registration createAndRegister(String orderId, String merchantId, String userId,
            ShippingAddress address, BigDecimal codAmount, BigDecimal shippingFee, String currency) {
        ShippingFulfillmentPort.RegistrationResult result = shippingFulfillmentPort.createAndRegister(
                orderId, merchantId, userId, toPortAddress(address), codAmount, shippingFee, currency);
        return new Registration(result.shipmentId(), result.trackingCode(),
                result.carrierOrderId(), result.labelUrl());
    }

    private ShippingFulfillmentPort.Address toPortAddress(ShippingAddress address) {
        if (address == null) {
            return null;
        }
        return new ShippingFulfillmentPort.Address(address.fullName(), address.phone(),
                address.addressLine(), address.wardCode(), address.districtCode(),
                address.provinceCode(), address.countryCode());
    }
}
