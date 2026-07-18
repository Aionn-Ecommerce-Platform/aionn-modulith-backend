package com.aionn.ordering.adapter.rest.dto.request;

import com.aionn.ordering.domain.valueobject.ShippingAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ChangeShippingInfoRequest(
        @NotNull @Valid ShippingAddress newAddress,
        @DecimalMin(value = "0.0") BigDecimal newShippingFee) {
}

