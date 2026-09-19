package com.aionn.ucp.adapter.rest.dto.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Price total entry representing subtotal, fulfillment, discount, or total amount.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpTotalResponse(
        @JsonProperty("type") String type,
        @JsonProperty("amount") long amount,
        @JsonProperty("display_text") String displayText) {

    public UcpTotalResponse(String type, long amount) {
        this(type, amount, null);
    }

    /**
     * Creates a subtotal entry.
     */
    public static UcpTotalResponse subtotal(long amount) {
        return new UcpTotalResponse("subtotal", amount, null);
    }

    /**
     * Creates a fulfillment entry with custom display text.
     */
    public static UcpTotalResponse fulfillment(long amount, String displayText) {
        return new UcpTotalResponse("fulfillment", amount, displayText);
    }

    /**
     * Creates a standard shipping fulfillment entry.
     */
    public static UcpTotalResponse shipping(long amount) {
        return fulfillment(amount, "Shipping");
    }

    /**
     * Creates a grand total entry.
     */
    public static UcpTotalResponse total(long amount) {
        return new UcpTotalResponse("total", amount, null);
    }
}
