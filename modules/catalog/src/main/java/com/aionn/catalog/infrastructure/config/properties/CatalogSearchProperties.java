package com.aionn.catalog.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "catalog.search")
public record CatalogSearchProperties(
        @DefaultValue("in-process") String provider,
        @DefaultValue OpenSearch opensearch) {

    public record OpenSearch(
            @DefaultValue("localhost") String host,
            @DefaultValue("9200") int port,
            @DefaultValue("http") String scheme,
            @DefaultValue("catalog-products") String indexName,
            @DefaultValue("") String username,
            @DefaultValue("") String password) {

        public boolean hasCredentials() {
            return username != null && !username.isBlank()
                    && password != null && !password.isBlank();
        }
    }
}
