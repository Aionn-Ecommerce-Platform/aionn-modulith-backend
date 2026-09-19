package com.aionn.ucp.adapter.rest.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.aionn.ucp.adapter.rest.dto.UcpErrorResponse;
import com.aionn.ucp.infrastructure.config.UcpProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
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
}
