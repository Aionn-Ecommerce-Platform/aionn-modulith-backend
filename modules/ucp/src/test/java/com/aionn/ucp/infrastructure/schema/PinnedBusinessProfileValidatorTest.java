package com.aionn.ucp.infrastructure.schema;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aionn.ucp.adapter.rest.controller.DiscoveryController;
import com.aionn.ucp.adapter.rest.dto.BusinessProfileResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PinnedBusinessProfileValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final PinnedBusinessProfileValidator validator = new PinnedBusinessProfileValidator();

    @Test
    void validatesDiscoveryUsingRuntimeResources() {
        assertThatCode(() -> validator.validate(mapper.valueToTree(BusinessProfileResponse.discoveryOnly())))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidProfileWithoutIncludingPayloadInError() {
        ObjectNode profile = mapper.valueToTree(BusinessProfileResponse.discoveryOnly());
        ((ObjectNode) profile.get("ucp")).put("version", "sensitive-invalid-value");
        assertThatThrownBy(() -> validator.validate(profile))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("UCP business profile does not match the pinned schema");
    }

    @Test
    void preventsDiscoveryConstructionWhenValidationFails() {
        assertThatThrownBy(() -> new DiscoveryController(profile -> {
            throw new IllegalStateException("Invalid profile");
        })).isInstanceOf(IllegalStateException.class).hasMessage("Invalid profile");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com/schema.json",
            "https://ucp.dev/schemas/unknown.json",
            "https://ucp.dev/schemas/../profile.json",
            "file:///profile.json",
            "classpath:profile.json"
    })
    void rejectsUnregisteredSchemaLocations(String uri) {
        assertThatThrownBy(() -> PinnedBusinessProfileValidator.loadSchema(uri))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Schema is not in the pinned discovery registry");
    }
}
