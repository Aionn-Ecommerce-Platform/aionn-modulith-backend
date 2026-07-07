package com.aionn.catalog.infrastructure.config;

import com.aionn.catalog.infrastructure.config.properties.CatalogCloudinaryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CatalogCloudinaryProperties.class)
public class CatalogMediaConfig {
}
