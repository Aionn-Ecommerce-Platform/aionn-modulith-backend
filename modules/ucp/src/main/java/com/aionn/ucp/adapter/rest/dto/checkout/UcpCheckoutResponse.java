package com.aionn.ucp.adapter.rest.dto.checkout;

import com.aionn.ucp.adapter.rest.dto.UcpMessage;
import com.aionn.ucp.adapter.rest.dto.cart.UcpLineItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpResponseMetadata;
import com.aionn.ucp.adapter.rest.dto.cart.UcpTotalResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpCheckoutResponse(
        @JsonProperty("ucp") UcpResponseMetadata ucp,
        @JsonProperty("id") String id,
        @JsonProperty("status") String status,
        @JsonProperty("currency") String currency,
        @JsonProperty("line_items") List<UcpLineItemResponse> lineItems,
        @JsonProperty("totals") List<UcpTotalResponse> totals,
        @JsonProperty("links") List<UcpLinkResponse> links,
        @JsonProperty("buyer") Map<String, Object> buyer,
        @JsonProperty("context") Map<String, Object> context,
        @JsonProperty("order") UcpOrderConfirmationResponse order,
        @JsonProperty("expires_at") String expiresAt,
        @JsonProperty("messages") List<UcpMessage> messages) {
}
