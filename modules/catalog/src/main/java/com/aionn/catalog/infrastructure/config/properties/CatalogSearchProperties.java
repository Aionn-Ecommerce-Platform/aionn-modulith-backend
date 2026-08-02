package com.aionn.catalog.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

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
            @DefaultValue("") String password,
            @DefaultValue("3s") Duration connectTimeout,
            @DefaultValue("10s") Duration responseTimeout,
            @DefaultValue("1") int indexShards,
            @DefaultValue("0") int indexReplicas,
            @DefaultValue("2") int ngramMin,
            @DefaultValue("15") int ngramMax,
            @DefaultValue("50") int facetBrandSize,
            @DefaultValue("100") int facetCategorySize,
            @DefaultValue("50") int facetAttributeSize) {

        public boolean hasCredentials() {
            return username != null && !username.isBlank()
                    && password != null && !password.isBlank();
        }
    }
}
