package com.aionn.ordering.adapter.rest.dto.request;

import com.aionn.ordering.domain.valueobject.ShippingAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaceOrderRequest(
        @NotBlank String addressId,
        String paymentMethodId,
        @Size(min = 3, max = 3) String currency,
        @NotNull @Valid ShippingAddress shippingAddress,
        List<String> selectedSkuIds,
        @NotBlank @Pattern(regexp = "STRIPE|VNPAY|COD",
                message = "gateway must be one of: STRIPE, VNPAY, COD")
        String gateway,
        @Size(max = 1000, message = "note must be at most 1000 characters")
        String note,
        @Size(max = 20, message = "shippingService must be at most 20 characters")
        String shippingService) {
}
