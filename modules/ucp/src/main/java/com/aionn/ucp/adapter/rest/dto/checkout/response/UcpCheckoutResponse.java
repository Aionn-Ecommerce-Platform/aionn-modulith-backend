package com.aionn.ucp.adapter.rest.dto.checkout.response;

import com.aionn.ucp.adapter.rest.dto.UcpMessage;
import com.aionn.ucp.adapter.rest.dto.cart.response.UcpLineItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpResponseMetadata;
import com.aionn.ucp.adapter.rest.dto.cart.response.UcpTotalResponse;
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
                @JsonProperty("payment") Map<String, Object> payment,
                @JsonProperty("fulfillment") Map<String, Object> fulfillment,
                @JsonProperty("order") UcpOrderConfirmationResponse order,
                @JsonProperty("expires_at") String expiresAt,
                @JsonProperty("messages") List<UcpMessage> messages) {

        public UcpCheckoutResponse(
                        UcpResponseMetadata ucp,
                        String id,
                        String status,
                        String currency,
                        List<UcpLineItemResponse> lineItems,
                        List<UcpTotalResponse> totals,
                        List<UcpLinkResponse> links,
                        Map<String, Object> buyer,
                        Map<String, Object> context,
                        UcpOrderConfirmationResponse order,
                        String expiresAt,
                        List<UcpMessage> messages) {
                this(ucp, id, status, currency, lineItems, totals, links, buyer, context, null, null, order,
                        expiresAt, messages);
        }
}
