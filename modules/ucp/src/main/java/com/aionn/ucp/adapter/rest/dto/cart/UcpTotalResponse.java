package com.aionn.ucp.adapter.rest.dto.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpTotalResponse(
        @JsonProperty("type") String type,
        @JsonProperty("amount") long amount) {

    public static UcpTotalResponse subtotal(long amount) {
        return new UcpTotalResponse("subtotal", amount);
    }

    public static UcpTotalResponse total(long amount) {
        return new UcpTotalResponse("total", amount);
    }
}
