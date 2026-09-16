package com.aionn.ucp.adapter.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.aionn.ucp.infrastructure.schema.PinnedBusinessProfileValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.net.URI;
import org.junit.jupiter.api.Test;

class DiscoveryControllerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012,
            builder -> builder.schemaMappers(mappers -> mappers.mapPrefix(
                    "https://ucp.dev/schemas/", "classpath:ucp/2026-08-25/")))
            .getSchema(URI.create("classpath:ucp/2026-08-25/profile.json#/$defs/business_schema"));

    @Test
    void discoveryMatchesPinnedBusinessSchemaWithoutAdvertisingUnimplementedCapabilities() {
        ObjectNode profile = mapper.valueToTree(
                new DiscoveryController(new PinnedBusinessProfileValidator()).getProfile());

        assertThat(schema.validate(profile)).isEmpty();
        assertThat(profile.at("/ucp/version").asText()).isEqualTo("2026-08-25");
        assertThat(profile.at("/ucp/services").isEmpty()).isTrue();
        assertThat(profile.at("/ucp/capabilities").isEmpty()).isTrue();
        assertThat(profile.at("/ucp/payment_handlers").isObject()).isTrue();
        assertThat(profile.at("/ucp/payment_handlers").isEmpty()).isTrue();
    }

    @Test
    void businessSchemaRequiresPaymentHandlerRegistryEvenWhenEmpty() {
        ObjectNode profile = mapper.valueToTree(
                new DiscoveryController(new PinnedBusinessProfileValidator()).getProfile());
        ((ObjectNode) profile.get("ucp")).remove("payment_handlers");

        assertThat(schema.validate(profile)).isNotEmpty();
    }

    @Test
    void businessSchemaRejectsDraftAsWireVersion() {
        ObjectNode profile = mapper.valueToTree(
                new DiscoveryController(new PinnedBusinessProfileValidator()).getProfile());
        ((ObjectNode) profile.get("ucp")).put("version", "draft");

        assertThat(schema.validate(profile)).isNotEmpty();
    }
}
