package com.aionn.ucp.adapter.rest.controller;

import com.aionn.ucp.adapter.rest.dto.BusinessProfileResponse;
import com.aionn.ucp.application.port.out.BusinessProfileValidationPort;
import com.aionn.ucp.infrastructure.config.UcpProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiscoveryController {

    private final BusinessProfileResponse profile;

    @Autowired
    public DiscoveryController(BusinessProfileValidationPort validator,
            @Autowired(required = false) UcpProperties properties) {
        String version = properties != null ? properties.version() : BusinessProfileResponse.PROTOCOL_VERSION;
        Map<String, Object> services = new java.util.LinkedHashMap<>();
        Map<String, Object> capabilities = new java.util.LinkedHashMap<>();

        boolean cartEnabled = properties != null && properties.capabilities() != null && properties.capabilities().cart();
        boolean checkoutEnabled = properties != null && properties.capabilities() != null && properties.capabilities().checkout();

        if (cartEnabled || checkoutEnabled) {
            String endpoint = properties.restEndpoint() != null ? properties.restEndpoint()
                    : "http://localhost:8080/ucp/v1";
            services.put("dev.ucp.shopping", java.util.List.of(
                    Map.of(
                            "version", version,
                            "transport", "rest",
                            "endpoint", endpoint)));
        }

        if (cartEnabled) {
            capabilities.put("dev.ucp.shopping.cart", java.util.List.of(
                    Map.of(
                            "version", version,
                            "schema", "https://ucp.dev/schemas/shopping/cart.json")));
        }

        if (checkoutEnabled) {
            capabilities.put("dev.ucp.shopping.checkout", java.util.List.of(
                    Map.of(
                            "version", version,
                            "schema", "https://ucp.dev/schemas/shopping/checkout.json")));
        }

        profile = BusinessProfileResponse.of(version, services, capabilities, Map.of());
        validator.validate(new ObjectMapper().valueToTree(profile));
    }

    public DiscoveryController(BusinessProfileValidationPort validator) {
        this(validator, null);
    }

    @GetMapping(value = "/.well-known/ucp", produces = MediaType.APPLICATION_JSON_VALUE)
    public BusinessProfileResponse getProfile() {
        // Advertise no commerce services until their complete protocol flows are
        // available.
        return profile;
    }
}
