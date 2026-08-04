package com.aionn.ordering.adapter.rest.dto.request;

import com.aionn.ordering.domain.valueobject.ShippingAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaceOrderRequest(
        @NotBlank String addressId,
        String paymentMethodId,
        @Size(min = 3, max = 3) String currency,
        @Valid ShippingAddress shippingAddress,
        List<String> selectedSkuIds,
        @NotBlank @Pattern(regexp = "STRIPE|VNPAY|COD",
                message = "gateway must be one of: STRIPE, VNPAY, COD")
        String gateway) {
}
