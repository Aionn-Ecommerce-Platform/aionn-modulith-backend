package com.aionn.ucp.adapter.rest.advice;

import static org.assertj.core.api.Assertions.assertThat;

import com.aionn.ucp.adapter.rest.dto.UcpErrorResponse;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import com.aionn.ucp.infrastructure.config.UcpProperties;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class UcpControllerAdviceTest {

    private UcpControllerAdvice advice;

    @BeforeEach
    void setUp() {
        UcpProperties properties = new UcpProperties(
                "2026-08-25",
                "http://localhost:8080/ucp/v1",
                "Aionn Test",
                Set.of("https://ucp.dev/schemas/"),
                false,
                new UcpProperties.Capabilities(false, false, false, false));
        advice = new UcpControllerAdvice(properties);
    }

    @Test
    void translatesUcpProtocolException() {
        UcpProtocolException ex = new UcpProtocolException(
                400,
                "item_unavailable",
                "The requested SKU is out of stock",
                "recoverable",
                "$.line_items[0]");

        ResponseEntity<UcpErrorResponse> response = advice.handleUcpProtocolException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        UcpErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.ucp().status()).isEqualTo("error");
        assertThat(body.ucp().version()).isEqualTo("2026-08-25");
        assertThat(body.messages()).hasSize(1);
        assertThat(body.messages().get(0).code()).isEqualTo("item_unavailable");
        assertThat(body.messages().get(0).content()).isEqualTo("The requested SKU is out of stock");
        assertThat(body.messages().get(0).severity()).isEqualTo("recoverable");
        assertThat(body.messages().get(0).path()).isEqualTo("$.line_items[0]");
    }

    @Test
    void translatesMalformedJson() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "Malformed JSON", (org.springframework.http.HttpInputMessage) null);

        ResponseEntity<UcpErrorResponse> response = advice.handleMalformedJson(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().messages().get(0).code()).isEqualTo("invalid_request");
    }

    @Test
    void translatesUnsupportedMediaType() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException("text/xml");

        ResponseEntity<UcpErrorResponse> response = advice.handleUnsupportedMediaType(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().messages().get(0).code()).isEqualTo("unsupported_media_type");
    }

    @Test
    void translatesMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<UcpErrorResponse> response = advice.handleMethodNotSupported(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().messages().get(0).code()).isEqualTo("method_not_allowed");
    }

    @Test
    void translatesNotFound() {
        NoResourceFoundException ex = new NoResourceFoundException(
                org.springframework.http.HttpMethod.GET, "/ucp/v1/unknown", "Resource not found");

        ResponseEntity<UcpErrorResponse> response = advice.handleNoResourceFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().messages().get(0).code()).isEqualTo("not_found");
    }

    @Test
    void translatesGenericExceptionWithoutExposingInternalDetails() {
        RuntimeException ex = new RuntimeException("Sensitive database connection leak password=xyz");

        ResponseEntity<UcpErrorResponse> response = advice.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        UcpErrorResponse body = response.getBody();
        assertThat(body.messages().get(0).code()).isEqualTo("internal_error");
        assertThat(body.messages().get(0).content())
                .doesNotContain("password")
                .doesNotContain("database")
                .isEqualTo("An unexpected internal error occurred while processing the UCP operation");
    }
}
