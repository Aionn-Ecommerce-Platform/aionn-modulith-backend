package com.aionn.ucp.adapter.rest.dto.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpResponseMetadata(
        @JsonProperty("version") String version,
        @JsonProperty("status") String status,
        @JsonProperty("capabilities") Map<String, List<Map<String, Object>>> capabilities,
        @JsonProperty("payment_handlers") Map<String, List<Map<String, Object>>> paymentHandlers) {

    public UcpResponseMetadata(String version, String status,
            Map<String, List<Map<String, Object>>> capabilities) {
        this(version, status, capabilities, null);
    }

    public static UcpResponseMetadata success(String version) {
        return new UcpResponseMetadata(version, "success", null, null);
    }

    /**
     * Checkout responses must declare the payment handlers they accept; the pinned checkout schema
     * makes {@code payment_handlers} a required member of the {@code ucp} object.
     */
    public static UcpResponseMetadata checkoutSuccess(String version) {
        Map<String, List<Map<String, Object>>> handlers = Map.of(
                "vn.aionn.payment.cod", List.of(handler(version, "cod")),
                "vn.aionn.payment.vnpay", List.of(handler(version, "vnpay")),
                "vn.aionn.payment.stripe", List.of(handler(version, "stripe")));
        return new UcpResponseMetadata(version, "success", null, handlers);
    }

    private static Map<String, Object> handler(String version, String id) {
        return Map.of(
                "version", version,
                "id", id);
    }
}
