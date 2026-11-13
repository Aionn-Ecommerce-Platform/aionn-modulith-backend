package com.aionn.ucp.adapter.rest.dto.cart.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Price total entry: subtotal, fulfillment, discount or total. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpTotalResponse(
        @JsonProperty("type") String type,
        @JsonProperty("amount") long amount,
        @JsonProperty("display_text") String displayText) {

    public UcpTotalResponse(String type, long amount) {
        this(type, amount, null);
    }

    public static UcpTotalResponse subtotal(long amount) {
        return new UcpTotalResponse("subtotal", amount, null);
    }

    public static UcpTotalResponse fulfillment(long amount, String displayText) {
        return new UcpTotalResponse("fulfillment", amount, displayText);
    }

    public static UcpTotalResponse shipping(long amount) {
        return fulfillment(amount, "Shipping");
    }

    public static UcpTotalResponse discount(long amount) {
        return new UcpTotalResponse("discount", amount, "Discount");
    }

    public static UcpTotalResponse total(long amount) {
        return new UcpTotalResponse("total", amount, null);
    }
}
