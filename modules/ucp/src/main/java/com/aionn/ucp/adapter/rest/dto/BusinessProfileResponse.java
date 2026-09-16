package com.aionn.ucp.adapter.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record BusinessProfileResponse(Metadata ucp) {

    public static final String PROTOCOL_VERSION = "2026-08-25";

    public static BusinessProfileResponse discoveryOnly() {
        return new BusinessProfileResponse(new Metadata(PROTOCOL_VERSION, Map.of(), Map.of(), Map.of()));
    }

    public record Metadata(
            String version,
            Map<String, Object> services,
            Map<String, Object> capabilities,
            @JsonProperty("payment_handlers") Map<String, Object> paymentHandlers) {
        public Metadata {
            services = Map.copyOf(services);
            capabilities = Map.copyOf(capabilities);
            paymentHandlers = Map.copyOf(paymentHandlers);
        }
    }
}
