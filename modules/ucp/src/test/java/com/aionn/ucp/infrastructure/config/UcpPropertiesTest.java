package com.aionn.ucp.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class UcpPropertiesTest {

    @Test
    void defaultValuesAreConsistentWithPhase1() {
        UcpProperties properties = new UcpProperties(
                "2026-08-25",
                "http://localhost:8080/ucp/v1",
                "Aionn Commerce",
                Set.of("https://ucp.dev/schemas/"),
                false,
                new UcpProperties.Capabilities(false, false, false, false));

        assertThat(properties.version()).isEqualTo("2026-08-25");
        assertThat(properties.restEndpoint()).isEqualTo("http://localhost:8080/ucp/v1");
        assertThat(properties.businessName()).isEqualTo("Aionn Commerce");
        assertThat(properties.allowedSchemaPrefixes()).contains("https://ucp.dev/schemas/");
        assertThat(properties.requireHttps()).isFalse();
        assertThat(properties.capabilities().cart()).isFalse();
        assertThat(properties.capabilities().checkout()).isFalse();
        assertThat(properties.capabilities().catalog()).isFalse();
        assertThat(properties.capabilities().order()).isFalse();
    }
}
