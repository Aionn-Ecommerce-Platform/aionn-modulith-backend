package com.aionn.ucp.adapter.rest.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.aionn.ucp.adapter.rest.dto.UcpErrorResponse;
import com.aionn.ucp.application.port.out.UcpMetricsPort;
import com.aionn.ucp.infrastructure.config.UcpProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class UcpHeaderFilterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private UcpProperties properties;
    private UcpHeaderFilter filter;

    @BeforeEach
    void setUp() {
        properties = new UcpProperties(
                "2026-08-25",
                "http://localhost:8080/ucp/v1",
                "Aionn Test",
                Set.of("https://ucp.dev/schemas/"),
                false,
                new UcpProperties.Capabilities(false, false, false, false, false));
        filter = new UcpHeaderFilter(properties);
    }

    @Test
    void generatesRequestIdWhenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/.well-known/ucp");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String requestId = response.getHeader(UcpHeaderFilter.HEADER_REQUEST_ID);
        assertThat(requestId).isNotBlank();
    }

    @Test
    void preservesValidIncomingRequestId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/.well-known/ucp");
        request.addHeader(UcpHeaderFilter.HEADER_REQUEST_ID, "req-valid-12345");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(UcpHeaderFilter.HEADER_REQUEST_ID)).isEqualTo("req-valid-12345");
    }

    @Test
    void sanitizesAndStoresUcpAgent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/.well-known/ucp");
        request.addHeader(UcpHeaderFilter.HEADER_UCP_AGENT, "Google-Agent/1.0 (Linux; Android)");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute(UcpHeaderFilter.ATTR_UCP_AGENT))
                .isEqualTo("Google-Agent/1.0 (Linux; Android)");
    }

    @Test
    void enforcesJsonContentTypeOnMutatingUcpOperations() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ucp/v1/carts");
        request.setContentType("text/plain");
        request.setContent("hello".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(415);
        UcpErrorResponse error = mapper.readValue(response.getContentAsString(), UcpErrorResponse.class);
        assertThat(error.ucp().status()).isEqualTo("error");
        assertThat(error.messages()).hasSize(1);
        assertThat(error.messages().get(0).code()).isEqualTo("unsupported_media_type");
    }

    @Test
    void allowsJsonContentTypeWithCharsetOnMutatingOperations() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ucp/v1/carts");
        request.setContentType("application/json;charset=UTF-8");
        request.setContent("{}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void ignoresNonUcpRequests() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/catalog/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(UcpHeaderFilter.HEADER_REQUEST_ID)).isNull();
    }

    @Test
    void enforcesHttpsWhenConfigured() throws ServletException, IOException {
        UcpProperties secureProperties = new UcpProperties(
                "2026-08-25",
                "https://api.aionn.com/ucp/v1",
                "Aionn Prod",
                Set.of("https://ucp.dev/schemas/"),
                true, // requireHttps = true
                new UcpProperties.Capabilities(false, false, false, false, false));

        UcpHeaderFilter secureFilter = new UcpHeaderFilter(secureProperties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/.well-known/ucp");
        request.setSecure(false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        secureFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        UcpErrorResponse error = mapper.readValue(response.getContentAsString(), UcpErrorResponse.class);
        assertThat(error.messages().get(0).code()).isEqualTo("https_required");
    }

    @Test
    void allowsHttpsWhenConfiguredAndSecure() throws ServletException, IOException {
        UcpProperties secureProperties = new UcpProperties(
                "2026-08-25",
                "https://api.aionn.com/ucp/v1",
                "Aionn Prod",
                Set.of("https://ucp.dev/schemas/"),
                true,
                new UcpProperties.Capabilities(false, false, false, false, false));

        UcpHeaderFilter secureFilter = new UcpHeaderFilter(secureProperties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/.well-known/ucp");
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        secureFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsMalformedContentTypeOnMutatingOperations() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ucp/v1/carts");
        request.setContentType("invalid/;;;");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(415);
    }

    @Test
    void rejectsMissingContentTypeOnMutatingOperations() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ucp/v1/carts");
        request.setContentType(null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(415);
    }

    @Test
    void filterInitializesWithNullProperties() throws ServletException, IOException {
        UcpHeaderFilter filterWithNullProperties = new UcpHeaderFilter(null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/.well-known/ucp");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filterWithNullProperties.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void recordsMetricsOnSuccessfulRequest() throws ServletException, IOException {
        UcpMetricsPort metricsPort = mock(UcpMetricsPort.class);
        UcpHeaderFilter filterWithMetrics = new UcpHeaderFilter(properties, Optional.of(metricsPort));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ucp/v1/carts");
        request.setContentType("application/json");
        request.setContent("{}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filterWithMetrics.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(metricsPort).recordRequest("cart", "create", "success");
        verify(metricsPort).recordLatency(eq("cart"), eq("create"), any(Duration.class));
    }

    @Test
    void recordsMetricsOnClientError() throws ServletException, IOException {
        UcpMetricsPort metricsPort = mock(UcpMetricsPort.class);
        UcpHeaderFilter filterWithMetrics = new UcpHeaderFilter(properties, Optional.of(metricsPort));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ucp/v1/carts");
        request.setContentType(null); // missing content-type causes 415
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filterWithMetrics.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(415);
        verify(metricsPort).recordRequest("cart", "create", "client_error");
        verify(metricsPort).recordLatency(eq("cart"), eq("create"), any(Duration.class));
    }

    @Test
    void recordsMetricsOnDiscoveryEndpoint() throws ServletException, IOException {
        UcpMetricsPort metricsPort = mock(UcpMetricsPort.class);
        UcpHeaderFilter filterWithMetrics = new UcpHeaderFilter(properties, Optional.of(metricsPort));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/.well-known/ucp");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filterWithMetrics.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(metricsPort).recordRequest("discovery", "lookup", "success");
        verify(metricsPort).recordLatency(eq("discovery"), eq("lookup"), any(Duration.class));
    }

    @Test
    void recordsMetricsForCartGetPutCancel() throws ServletException, IOException {
        UcpMetricsPort metricsPort = mock(UcpMetricsPort.class);
        UcpHeaderFilter filterWithMetrics = new UcpHeaderFilter(properties, Optional.of(metricsPort));

        // GET cart
        MockHttpServletRequest getReq = new MockHttpServletRequest("GET", "/ucp/v1/carts/cart-1");
        filterWithMetrics.doFilter(getReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("cart", "get", "success");

        // PUT cart
        MockHttpServletRequest putReq = new MockHttpServletRequest("PUT", "/ucp/v1/carts/cart-1");
        putReq.setContentType("application/json");
        filterWithMetrics.doFilter(putReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("cart", "update", "success");

        // Cancel cart
        MockHttpServletRequest cancelReq = new MockHttpServletRequest("POST", "/ucp/v1/carts/cart-1/cancel");
        cancelReq.setContentType("application/json");
        filterWithMetrics.doFilter(cancelReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("cart", "cancel", "success");
    }

    @Test
    void recordsMetricsForCheckoutSessions() throws ServletException, IOException {
        UcpMetricsPort metricsPort = mock(UcpMetricsPort.class);
        UcpHeaderFilter filterWithMetrics = new UcpHeaderFilter(properties, Optional.of(metricsPort));

        // POST checkout create
        MockHttpServletRequest createReq = new MockHttpServletRequest("POST", "/ucp/v1/checkout-sessions");
        createReq.setContentType("application/json");
        filterWithMetrics.doFilter(createReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("checkout", "create", "success");

        // GET checkout
        MockHttpServletRequest getReq = new MockHttpServletRequest("GET", "/ucp/v1/checkout-sessions/cs-1");
        filterWithMetrics.doFilter(getReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("checkout", "get", "success");

        // PUT checkout update
        MockHttpServletRequest putReq = new MockHttpServletRequest("PUT", "/ucp/v1/checkout-sessions/cs-1");
        putReq.setContentType("application/json");
        filterWithMetrics.doFilter(putReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("checkout", "update", "success");

        // Complete checkout
        MockHttpServletRequest completeReq = new MockHttpServletRequest("POST",
                "/ucp/v1/checkout-sessions/cs-1/complete");
        completeReq.setContentType("application/json");
        filterWithMetrics.doFilter(completeReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("checkout", "complete", "success");

        // Cancel checkout
        MockHttpServletRequest cancelReq = new MockHttpServletRequest("POST", "/ucp/v1/checkout-sessions/cs-1/cancel");
        cancelReq.setContentType("application/json");
        filterWithMetrics.doFilter(cancelReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("checkout", "cancel", "success");
    }

    @Test
    void recordsMetricsForCatalogEndpoints() throws ServletException, IOException {
        UcpMetricsPort metricsPort = mock(UcpMetricsPort.class);
        UcpHeaderFilter filterWithMetrics = new UcpHeaderFilter(properties, Optional.of(metricsPort));

        // Search
        MockHttpServletRequest searchReq = new MockHttpServletRequest("POST", "/ucp/v1/catalog/search");
        searchReq.setContentType("application/json");
        filterWithMetrics.doFilter(searchReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("catalog", "search", "success");

        // Lookup
        MockHttpServletRequest lookupReq = new MockHttpServletRequest("POST", "/ucp/v1/catalog/lookup");
        lookupReq.setContentType("application/json");
        filterWithMetrics.doFilter(lookupReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("catalog", "lookup", "success");

        // Product
        MockHttpServletRequest productReq = new MockHttpServletRequest("POST", "/ucp/v1/catalog/product");
        productReq.setContentType("application/json");
        filterWithMetrics.doFilter(productReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("catalog", "product", "success");

        // Unknown catalog path
        MockHttpServletRequest unknownReq = new MockHttpServletRequest("GET", "/ucp/v1/catalog/other");
        filterWithMetrics.doFilter(unknownReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("catalog", "unknown", "success");
    }

    @Test
    void recordsMetricsForIdentityAndOrderEndpoints() throws ServletException, IOException {
        UcpMetricsPort metricsPort = mock(UcpMetricsPort.class);
        UcpHeaderFilter filterWithMetrics = new UcpHeaderFilter(properties, Optional.of(metricsPort));

        // Identity link
        MockHttpServletRequest linkReq = new MockHttpServletRequest("POST", "/ucp/v1/identity/link");
        linkReq.setContentType("application/json");
        filterWithMetrics.doFilter(linkReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("identity_linking", "link", "success");

        // Identity get
        MockHttpServletRequest getLinkReq = new MockHttpServletRequest("GET", "/ucp/v1/identity/link");
        filterWithMetrics.doFilter(getLinkReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("identity_linking", "get", "success");

        // Identity revoke
        MockHttpServletRequest revokeReq = new MockHttpServletRequest("DELETE", "/ucp/v1/identity/link");
        filterWithMetrics.doFilter(revokeReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("identity_linking", "revoke", "success");

        // Orders get
        MockHttpServletRequest orderReq = new MockHttpServletRequest("GET", "/ucp/v1/orders/ord-1");
        filterWithMetrics.doFilter(orderReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("order", "get", "success");

        // Unmatched path
        MockHttpServletRequest otherReq = new MockHttpServletRequest("GET", "/ucp/v1/unknown");
        filterWithMetrics.doFilter(otherReq, new MockHttpServletResponse(), new MockFilterChain());
        verify(metricsPort).recordRequest("unknown", "unknown", "success");
    }

    @Test
    void recordsMetricsOnServerErrorStatus() throws ServletException, IOException {
        UcpMetricsPort metricsPort = mock(UcpMetricsPort.class);
        UcpHeaderFilter filterWithMetrics = new UcpHeaderFilter(properties, Optional.of(metricsPort));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ucp/v1/orders/ord-500");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                ((HttpServletResponse) res).setStatus(500);
            }
        };

        filterWithMetrics.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(500);
        verify(metricsPort).recordRequest("order", "get", "server_error");
    }
}
