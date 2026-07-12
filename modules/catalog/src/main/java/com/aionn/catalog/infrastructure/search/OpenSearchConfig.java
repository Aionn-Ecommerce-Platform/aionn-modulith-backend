package com.aionn.catalog.infrastructure.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import com.aionn.catalog.infrastructure.config.properties.CatalogSearchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "catalog.search", name = "provider", havingValue = "opensearch")
@EnableConfigurationProperties(CatalogSearchProperties.class)
public class OpenSearchConfig {

    @Value("${catalog.search.opensearch.host:localhost}")
    private String host;

    @Value("${catalog.search.opensearch.port:9200}")
    private int port;

    @Value("${catalog.search.opensearch.scheme:http}")
    private String scheme;

    @Bean
    public OpenSearchClient openSearchClient() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OpenSearchTransport transport = ApacheHttpClient5TransportBuilder
                .builder(new HttpHost(scheme, host, port))
                .setMapper(new JacksonJsonpMapper(objectMapper))
                .build();
        return new OpenSearchClient(transport);
    }
}
