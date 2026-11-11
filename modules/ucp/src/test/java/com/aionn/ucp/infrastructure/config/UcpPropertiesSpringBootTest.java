package com.aionn.ucp.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = UcpPropertiesSpringBootTest.TestConfig.class)
class UcpPropertiesSpringBootTest {

    @Configuration
    @EnableConfigurationProperties(UcpProperties.class)
    static class TestConfig {
    }

    @Autowired
    private UcpProperties properties;

    @Test
    void springBootBindsUcpPropertiesWithoutErrors() {
        assertThat(properties).isNotNull();
        assertThat(properties.capabilities()).isNotNull();
        // Production controllers back every advertised capability, so they are on
        // by default; deployments disable individual ones via UCP_CAPABILITY_*.
        assertThat(properties.capabilities().identityLinking()).isTrue();
        assertThat(properties.capabilities().cart()).isTrue();
        assertThat(properties.capabilities().checkout()).isTrue();
        assertThat(properties.capabilities().catalog()).isTrue();
        assertThat(properties.capabilities().order()).isTrue();
    }
}
