package com.aionn.ordering.application.dto.order.command;

import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.application.command.Command;

import java.util.List;

public record PlaceOrderCommand(
        String userId,
        String addressId,
        String paymentMethodId,
        String currency,
        ShippingAddress shippingAddressSnapshot,
        List<String> selectedSkuIds,
        String gateway,
        String idempotencyKey,
        String note,
        String shippingService) implements Command {

    public PlaceOrderCommand(String userId, String addressId, String paymentMethodId, String currency,
            ShippingAddress shippingAddressSnapshot, List<String> selectedSkuIds, String gateway) {
        this(userId, addressId, paymentMethodId, currency, shippingAddressSnapshot, selectedSkuIds,
                gateway, null, null, null);
    }

    public PlaceOrderCommand(String userId, String addressId, String paymentMethodId, String currency,
            ShippingAddress shippingAddressSnapshot, List<String> selectedSkuIds, String gateway,
            String idempotencyKey) {
        this(userId, addressId, paymentMethodId, currency, shippingAddressSnapshot, selectedSkuIds,
                gateway, idempotencyKey, null, null);
    }

    public PlaceOrderCommand(String userId, String addressId, String paymentMethodId, String currency,
            ShippingAddress shippingAddressSnapshot, List<String> selectedSkuIds, String gateway,
            String idempotencyKey, String note) {
        this(userId, addressId, paymentMethodId, currency, shippingAddressSnapshot, selectedSkuIds,
                gateway, idempotencyKey, note, null);
    }
}
