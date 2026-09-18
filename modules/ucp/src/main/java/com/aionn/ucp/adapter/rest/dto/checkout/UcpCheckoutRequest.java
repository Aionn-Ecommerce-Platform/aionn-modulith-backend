package com.aionn.ucp.adapter.rest.dto.checkout;

import com.aionn.ucp.adapter.rest.dto.cart.UcpLineItemRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UcpCheckoutRequest(
        @JsonProperty("cart_id") String cartId,
        @Valid @JsonProperty("line_items") List<UcpLineItemRequest> lineItems,
        @JsonProperty("buyer") Map<String, Object> buyer,
        @JsonProperty("context") Map<String, Object> context) {
}
