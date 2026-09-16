package com.aionn.catalog.infrastructure.search;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Result;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;

import com.aionn.catalog.infrastructure.config.properties.CatalogSearchProperties;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSearchConfigTest {

    private static final String INDEX_RESPONSE =
            "{\"_index\":\"catalog-products\",\"_id\":\"PROD_1\",\"_version\":1,\"result\":\"created\","
                    + "\"_shards\":{\"total\":2,\"successful\":1,\"failed\":0},\"_seq_no\":0,\"_primary_term\":1}";

    @Test
    void buildsOpenSearchClient() {
        CatalogSearchProperties properties = new CatalogSearchProperties("opensearch",
                new CatalogSearchProperties.OpenSearch("localhost", 9200, "http", "catalog-products", "", "",
                        Duration.ofSeconds(3), Duration.ofSeconds(10), 1, 0, 2, 15, 50, 100, 50));
        OpenSearchConfig config = new OpenSearchConfig(properties);

        OpenSearchClient client = config.openSearchClient();

        assertThat(client).isNotNull();
    }

    @Test
    void indexRoundTripDoesNotDecodeTheResponseTwice() throws IOException {
        // HttpClient 5.6 inflates a gzipped response itself but leaves the Content-Encoding header on the
        // message, and opensearch-java 2.13's transport reads that header and inflates the already-plain body
        // a second time - every index call then dies with "ZipException: Not in GZIP format", which
        // ResilientProductSearchIndex swallows, so the search index silently stays empty. OpenSearchConfig
        // turns the client's transparent decompression off; this drives a real round trip so the two sides
        // cannot start decoding twice again unnoticed.
        AtomicBoolean serverWasAskedForGzip = new AtomicBoolean(false);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String acceptEncoding = exchange.getRequestHeaders().getFirst("Accept-Encoding");
            boolean wantsGzip = acceptEncoding != null && acceptEncoding.toLowerCase().contains("gzip");
            serverWasAskedForGzip.set(wantsGzip);
            exchange.getRequestBody().readAllBytes();

            byte[] payload = INDEX_RESPONSE.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            if (wantsGzip) {
                ByteArrayOutputStream compressed = new ByteArrayOutputStream();
                try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
                    gzip.write(payload);
                }
                payload = compressed.toByteArray();
                exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            }
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(payload);
            }
            exchange.close();
        });
        server.start();
        try {
            CatalogSearchProperties properties = new CatalogSearchProperties("opensearch",
                    new CatalogSearchProperties.OpenSearch("127.0.0.1", server.getAddress().getPort(), "http",
                            "catalog-products", "", "", Duration.ofSeconds(3), Duration.ofSeconds(10),
                            1, 0, 2, 15, 50, 100, 50));
            OpenSearchClient client = new OpenSearchConfig(properties).openSearchClient();

            Map<String, Object> document = Map.of("name", "Widget");
            IndexRequest<Map<String, Object>> request = IndexRequest.of(builder -> builder
                    .index("catalog-products")
                    .id("PROD_1")
                    .document(document));

            IndexResponse response = client.index(request);

            assertThat(response.id()).isEqualTo("PROD_1");
            assertThat(response.result()).isEqualTo(Result.Created);
            // The transport only advertises gzip when setCompressionEnabled(true) is set, which we do not:
            // with the client's own decompression off, nothing on the wire may need inflating at all.
            assertThat(serverWasAskedForGzip).isFalse();
        } finally {
            server.stop(0);
        }
    }
}
