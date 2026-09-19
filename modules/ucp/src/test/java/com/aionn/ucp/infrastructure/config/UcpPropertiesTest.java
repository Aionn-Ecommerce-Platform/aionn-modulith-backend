package com.aionn.ucp.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UcpPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void defaultValuesAreConsistentWithPhase1() {
        UcpProperties properties = new UcpProperties(
                "2026-08-25",
                "http://localhost:8080/ucp/v1",
                "Aionn Commerce",
                Set.of("https://ucp.dev/schemas/"),
                false,
                new UcpProperties.Capabilities(false, false, false, false, false));

        assertThat(properties.version()).isEqualTo("2026-08-25");
        assertThat(properties.restEndpoint()).isEqualTo("http://localhost:8080/ucp/v1");
        assertThat(properties.businessName()).isEqualTo("Aionn Commerce");
        assertThat(properties.allowedSchemaPrefixes()).contains("https://ucp.dev/schemas/");
        assertThat(properties.requireHttps()).isFalse();
        assertThat(properties.capabilities().cart()).isFalse();
        assertThat(properties.capabilities().checkout()).isFalse();
        assertThat(properties.capabilities().catalog()).isFalse();
        assertThat(properties.capabilities().order()).isFalse();
        assertThat(properties.capabilities().identityLinking()).isFalse();

        Set<ConstraintViolation<UcpProperties>> violations = validator.validate(properties);
        assertThat(violations).isEmpty();
    }

    @Test
    void rejectsUnsupportedVersion() {
        UcpProperties properties = new UcpProperties(
                "2027-01-01",
                "http://localhost:8080/ucp/v1",
                "Aionn Commerce",
                Set.of("https://ucp.dev/schemas/"),
                false,
                new UcpProperties.Capabilities(false, false, false, false, false));

        Set<ConstraintViolation<UcpProperties>> violations = validator.validate(properties);
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("version"));
    }
}
