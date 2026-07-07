package com.aionn.catalog.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "catalog.media.cloudinary")
public record CatalogCloudinaryProperties(
        @DefaultValue("aionn/catalog/products") String productImageFolder,
        @DefaultValue("aionn/catalog/reviews") String reviewImageFolder) {
}
