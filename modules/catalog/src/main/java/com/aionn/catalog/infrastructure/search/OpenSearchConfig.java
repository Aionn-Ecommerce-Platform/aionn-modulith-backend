package com.aionn.catalog.infrastructure.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
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
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.of(config.connectTimeout()))
                .setSocketTimeout(Timeout.of(config.responseTimeout()))
                .build();
        OpenSearchTransport transport = ApacheHttpClient5TransportBuilder
                .builder(new HttpHost(config.scheme(), config.host(), config.port()))
                .setHttpClientConfigCallback(client -> client
                        .setConnectionManager(PoolingAsyncClientConnectionManagerBuilder.create()
                                .setDefaultConnectionConfig(connectionConfig)
                                .build())
                        .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.of(config.connectTimeout()))
                        .setResponseTimeout(Timeout.of(config.responseTimeout()))
                        .build())
                        // HttpClient 5.6 added ContentCompressionAsyncExec to the async chain: it advertises
                        // "Accept-Encoding: gzip" itself and inflates the response, but leaves the
                        // Content-Encoding header on the message. opensearch-java 2.13 was written against 5.3,
                        // whose async chain had no compression handling, so its transport reads that header and
                        // wraps the body in a second GzipDecompressingEntity - inflating plain JSON and failing
                        // every write with "ZipException: Not in GZIP format". Handing compression back to the
                        // transport is the only side that can opt out, and it only asks for gzip when
                        // setCompressionEnabled(true) is set, which we do not.
                        .disableContentCompression())
                .setMapper(new JacksonJsonpMapper(objectMapper))
                .build();
        return new OpenSearchClient(transport);
    }
}
