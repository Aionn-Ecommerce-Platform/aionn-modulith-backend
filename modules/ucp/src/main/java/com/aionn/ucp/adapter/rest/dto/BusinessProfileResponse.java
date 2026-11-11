package com.aionn.ucp.adapter.rest.dto;

import com.aionn.ucp.application.UcpProtocolConstants;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record BusinessProfileResponse(Metadata ucp) {

    public static final String PROTOCOL_VERSION = UcpProtocolConstants.VERSION;

    public static BusinessProfileResponse discoveryOnly() {
        return of(PROTOCOL_VERSION, Map.of(), Map.of(), Map.of());
    }

    public static BusinessProfileResponse of(
            String version,
            Map<String, Object> services,
            Map<String, Object> capabilities,
            Map<String, Object> paymentHandlers) {
        return of(version, services, capabilities, paymentHandlers, List.of());
    }

    public static BusinessProfileResponse of(
            String version,
            Map<String, Object> services,
            Map<String, Object> capabilities,
            Map<String, Object> paymentHandlers,
            List<Map<String, Object>> signingKeys) {
        return new BusinessProfileResponse(new Metadata(
                version != null ? version : PROTOCOL_VERSION,
                services != null ? services : Map.of(),
                capabilities != null ? capabilities : Map.of(),
                paymentHandlers != null ? paymentHandlers : Map.of(),
                signingKeys != null ? signingKeys : List.of()));
    }

    public record Metadata(
            String version,
            Map<String, Object> services,
            Map<String, Object> capabilities,
            @JsonProperty("payment_handlers") Map<String, Object> paymentHandlers,
            // Omitted entirely when no signing key is configured, so the profile
            // stays schema-valid instead of advertising an empty JWK Set.
            @JsonInclude(JsonInclude.Include.NON_EMPTY) List<Map<String, Object>> keys) {
        public Metadata {
            services = Map.copyOf(services);
            capabilities = Map.copyOf(capabilities);
            paymentHandlers = Map.copyOf(paymentHandlers);
            keys = keys == null ? List.of() : List.copyOf(keys);
        }
    }
}
