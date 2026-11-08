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

        private static final String KEY_VERSION = "version";
        private static final String KEY_SCHEMA = "schema";
        private final BusinessProfileResponse profile;

        @Autowired
        public DiscoveryController(BusinessProfileValidationPort validator,
                        @Autowired(required = false) UcpProperties properties) {
                String version = properties != null ? properties.version() : BusinessProfileResponse.PROTOCOL_VERSION;
                UcpProperties.Capabilities caps = (properties != null) ? properties.capabilities() : null;

                // Only advertise a REST endpoint when one is explicitly configured —
                // an empty or missing value would publish a wrong, unreachable URL to
                // every UCP agent that reads the discovery document.
                String endpoint = properties != null ? properties.restEndpoint() : null;
                if (endpoint == null || endpoint.isBlank()) {
                        caps = null; // cannot advertise services without a real endpoint
                }

                Map<String, Object> services = buildServices(caps, version, endpoint);
                Map<String, Object> capabilities = buildCapabilities(caps, version);

                profile = BusinessProfileResponse.of(version, services, capabilities, Map.of());
                validator.validate(new ObjectMapper().valueToTree(profile));
        }

        private static Map<String, Object> buildServices(UcpProperties.Capabilities caps, String version,
                        String endpoint) {
                Map<String, Object> services = new java.util.LinkedHashMap<>();
                if (caps == null) {
                        return services;
                }
                if (caps.cart() || caps.checkout() || caps.catalog() || caps.order()) {
                        services.put("dev.ucp.shopping", java.util.List.of(
                                        Map.of(KEY_VERSION, version, "transport", "rest", "endpoint", endpoint)));
                }
                if (caps.identityLinking()) {
                        services.put("dev.ucp.common", java.util.List.of(
                                        Map.of(KEY_VERSION, version, "transport", "rest", "endpoint", endpoint)));
                }
                return services;
        }

        private static Map<String, Object> buildCapabilities(UcpProperties.Capabilities caps, String version) {
                Map<String, Object> capabilities = new java.util.LinkedHashMap<>();
                if (caps == null) {
                        return capabilities;
                }
                if (caps.identityLinking()) {
                        capabilities.put("dev.ucp.common.identity_linking",
                                        capabilityEntry(version,
                                                        schemaUrl(version, "common/identity_linking.json")));
                }
                if (caps.cart()) {
                        capabilities.put("dev.ucp.shopping.cart",
                                        capabilityEntry(version, schemaUrl(version, "shopping/cart.json")));
                }
                if (caps.checkout()) {
                        capabilities.put("dev.ucp.shopping.checkout",
                                        capabilityEntry(version, schemaUrl(version, "shopping/checkout.json")));
                }
                if (caps.catalog()) {
                        capabilities.put("dev.ucp.shopping.catalog.search",
                                        capabilityEntry(version,
                                                        schemaUrl(version, "shopping/catalog_search.json")));
                        capabilities.put("dev.ucp.shopping.catalog.lookup",
                                        capabilityEntry(version,
                                                        schemaUrl(version, "shopping/catalog_lookup.json")));
                }
                if (caps.order()) {
                        capabilities.put("dev.ucp.shopping.order",
                                        capabilityEntry(version, schemaUrl(version, "shopping/order.json")));
                }
                return capabilities;
        }

        private static java.util.List<Map<String, Object>> capabilityEntry(String version, String schema) {
                return java.util.List.of(Map.of(KEY_VERSION, version, KEY_SCHEMA, schema));
        }

        private static String schemaUrl(String version, String path) {
                // Canonical schema prefix the pinned contract uses for its $refs
                // (/schemas/, not a versioned path) — keeps the discovery URLs
                // resolvable against the same bundle the wire validates.
                return "https://ucp.dev/schemas/" + path;
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
