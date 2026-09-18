package com.aionn.ucp.adapter.rest.dto.cart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UcpLineItemRequest(
        @JsonProperty("id") String id,
        @NotNull(message = "Item is required") @Valid @JsonProperty("item") UcpItemRequest item,
        @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") @JsonProperty("quantity") Integer quantity) {
}
