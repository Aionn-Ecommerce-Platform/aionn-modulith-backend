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

        @Test
        void discoveryAdvertisesCartCapabilityWhenConfigured() {
                com.aionn.ucp.infrastructure.config.UcpProperties properties = new com.aionn.ucp.infrastructure.config.UcpProperties(
                                "2026-08-25",
                                "http://localhost:8080/ucp/v1",
                                "Aionn Commerce",
                                java.util.Set.of("https://ucp.dev/schemas/"),
                                false,
                                new com.aionn.ucp.infrastructure.config.UcpProperties.Capabilities(true, false, false,
                                                false));

                DiscoveryController controller = new DiscoveryController(new PinnedBusinessProfileValidator(),
                                properties);
                ObjectNode profile = mapper.valueToTree(controller.getProfile());

                assertThat(schema.validate(profile)).isEmpty();
                assertThat(profile.at("/ucp/capabilities/dev.ucp.shopping.cart").isArray()).isTrue();
                assertThat(profile.at("/ucp/capabilities/dev.ucp.shopping.cart/0/schema").asText())
                                .isEqualTo("https://ucp.dev/schemas/shopping/cart.json");
                assertThat(profile.at("/ucp/services/dev.ucp.shopping/0/endpoint").asText())
                                .isEqualTo("http://localhost:8080/ucp/v1");
        }

        @Test
        void discoveryAdvertisesCheckoutCapabilityWhenConfigured() {
                com.aionn.ucp.infrastructure.config.UcpProperties properties = new com.aionn.ucp.infrastructure.config.UcpProperties(
                                "2026-08-25",
                                "http://localhost:8080/ucp/v1",
                                "Aionn Commerce",
                                java.util.Set.of("https://ucp.dev/schemas/"),
                                false,
                                new com.aionn.ucp.infrastructure.config.UcpProperties.Capabilities(false, true, false,
                                                false));

                DiscoveryController controller = new DiscoveryController(new PinnedBusinessProfileValidator(),
                                properties);
                ObjectNode profile = mapper.valueToTree(controller.getProfile());

                assertThat(schema.validate(profile)).isEmpty();
                assertThat(profile.at("/ucp/capabilities/dev.ucp.shopping.checkout").isArray()).isTrue();
                assertThat(profile.at("/ucp/capabilities/dev.ucp.shopping.checkout/0/schema").asText())
                                .isEqualTo("https://ucp.dev/schemas/shopping/checkout.json");
                assertThat(profile.at("/ucp/services/dev.ucp.shopping/0/endpoint").asText())
                                .isEqualTo("http://localhost:8080/ucp/v1");
        }

        @Test
        void discoveryAdvertisesCatalogCapabilityWhenConfigured() {
                com.aionn.ucp.infrastructure.config.UcpProperties properties = new com.aionn.ucp.infrastructure.config.UcpProperties(
                                "2026-08-25",
                                "http://localhost:8080/ucp/v1",
                                "Aionn Commerce",
                                java.util.Set.of("https://ucp.dev/schemas/"),
                                false,
                                new com.aionn.ucp.infrastructure.config.UcpProperties.Capabilities(false, false, true,
                                                false));

                DiscoveryController controller = new DiscoveryController(new PinnedBusinessProfileValidator(),
                                properties);
                ObjectNode profile = mapper.valueToTree(controller.getProfile());

                assertThat(schema.validate(profile)).isEmpty();
                assertThat(profile.at("/ucp/capabilities/dev.ucp.shopping.catalog.search").isArray()).isTrue();
                assertThat(profile.at("/ucp/capabilities/dev.ucp.shopping.catalog.search/0/schema").asText())
                                .isEqualTo("https://ucp.dev/schemas/shopping/catalog_search.json");
                assertThat(profile.at("/ucp/capabilities/dev.ucp.shopping.catalog.lookup").isArray()).isTrue();
                assertThat(profile.at("/ucp/capabilities/dev.ucp.shopping.catalog.lookup/0/schema").asText())
                                .isEqualTo("https://ucp.dev/schemas/shopping/catalog_lookup.json");
                assertThat(profile.at("/ucp/services/dev.ucp.shopping/0/endpoint").asText())
                                .isEqualTo("http://localhost:8080/ucp/v1");
        }

        @Test
        void discoveryAdvertisesOrderCapabilityWhenConfigured() {
                com.aionn.ucp.infrastructure.config.UcpProperties properties = new com.aionn.ucp.infrastructure.config.UcpProperties(
                                "2026-08-25",
                                "http://localhost:8080/ucp/v1",
                                "Aionn Commerce",
                                java.util.Set.of("https://ucp.dev/schemas/"),
                                false,
                                new com.aionn.ucp.infrastructure.config.UcpProperties.Capabilities(false, false, false,
                                                true));

                DiscoveryController controller = new DiscoveryController(new PinnedBusinessProfileValidator(),
                                properties);
                ObjectNode profile = mapper.valueToTree(controller.getProfile());

                assertThat(schema.validate(profile)).isEmpty();
                assertThat(profile.at("/ucp/capabilities/dev.ucp.shopping.order").isArray()).isTrue();
                assertThat(profile.at("/ucp/capabilities/dev.ucp.shopping.order/0/schema").asText())
                                .isEqualTo("https://ucp.dev/schemas/shopping/order.json");
                assertThat(profile.at("/ucp/services/dev.ucp.shopping/0/endpoint").asText())
                                .isEqualTo("http://localhost:8080/ucp/v1");
        }
}
