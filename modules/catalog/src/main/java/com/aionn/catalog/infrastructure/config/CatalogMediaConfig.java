package com.aionn.catalog.infrastructure.config;

import com.aionn.catalog.infrastructure.config.properties.CatalogCloudinaryProperties;
import com.aionn.catalog.infrastructure.config.properties.CatalogMediaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CatalogMediaProperties.class, CatalogCloudinaryProperties.class})
public class CatalogMediaConfig {
}
