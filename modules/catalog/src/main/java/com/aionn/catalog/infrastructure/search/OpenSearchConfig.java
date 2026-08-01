package com.aionn.catalog.infrastructure.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.client5.http.config.RequestConfig;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import com.aionn.catalog.infrastructure.config.properties.CatalogSearchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "catalog.search", name = "provider", havingValue = "opensearch")
@EnableConfigurationProperties(CatalogSearchProperties.class)
public class OpenSearchConfig {

    private final CatalogSearchProperties properties;

    public OpenSearchConfig(CatalogSearchProperties properties) {
        this.properties = properties;
    }

    @Bean
    public OpenSearchClient openSearchClient() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        CatalogSearchProperties.OpenSearch config = properties.opensearch();
        OpenSearchTransport transport = ApacheHttpClient5TransportBuilder
                .builder(new HttpHost(config.scheme(), config.host(), config.port()))
                .setHttpClientConfigCallback(client -> client.setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(Timeout.of(config.connectTimeout()))
                        .setConnectionRequestTimeout(Timeout.of(config.connectTimeout()))
                        .setResponseTimeout(Timeout.of(config.responseTimeout()))
                        .build()))
                .setMapper(new JacksonJsonpMapper(objectMapper))
                .build();
        return new OpenSearchClient(transport);
    }
}
