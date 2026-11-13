package com.aionn.ucp.adapter.rest.dto.checkout.request;

import com.aionn.ucp.adapter.rest.dto.cart.request.UcpLineItemRequest;
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
                @JsonProperty("context") Map<String, Object> context,
                @JsonProperty("payment") Map<String, Object> payment,
                @JsonProperty("fulfillment") Map<String, Object> fulfillment,
                @JsonProperty("voucher_code") String voucherCode) {

        public UcpCheckoutRequest(
                        String cartId,
                        List<UcpLineItemRequest> lineItems,
                        Map<String, Object> buyer,
                        Map<String, Object> context) {
                this(cartId, lineItems, buyer, context, null, null, null);
        }
}
