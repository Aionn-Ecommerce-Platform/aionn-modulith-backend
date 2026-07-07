package com.aionn.catalog.infrastructure.config;

import com.aionn.catalog.infrastructure.config.properties.CatalogProductProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
                CatalogProductProperties.class
})
public class CatalogPropertiesConfig {
}
