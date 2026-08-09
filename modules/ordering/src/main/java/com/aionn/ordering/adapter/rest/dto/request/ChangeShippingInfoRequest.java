package com.aionn.ordering.adapter.rest.dto.request;

import com.aionn.ordering.domain.valueobject.ShippingAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ChangeShippingInfoRequest(
        @NotNull @Valid ShippingAddress newAddress) {
}

