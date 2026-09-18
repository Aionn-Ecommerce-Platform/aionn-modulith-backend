package com.aionn.ucp.adapter.rest.dto.checkout;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpOrderConfirmationResponse(
        @JsonProperty("id") String id,
        @JsonProperty("label") String label,
        @JsonProperty("permalink_url") String permalinkUrl) {

    public static UcpOrderConfirmationResponse of(String orderId, String baseUrl) {
        return new UcpOrderConfirmationResponse(
                orderId,
                "Order #" + orderId,
                baseUrl + "/orders/" + orderId);
    }
}
