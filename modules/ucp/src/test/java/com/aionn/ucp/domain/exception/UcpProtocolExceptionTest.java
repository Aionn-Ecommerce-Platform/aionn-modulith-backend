package com.aionn.ucp.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UcpProtocolExceptionTest {

    @Test
    void constructsWithFourArguments() {
        UcpProtocolException ex = new UcpProtocolException(400, "invalid_payload", "Payload is invalid", "recoverable");

        assertThat(ex.getStatusCode()).isEqualTo(400);
        assertThat(ex.getErrorCode()).isEqualTo("invalid_payload");
        assertThat(ex.getMessage()).isEqualTo("Payload is invalid");
        assertThat(ex.getSeverity()).isEqualTo("recoverable");
        assertThat(ex.getPath()).isNull();
        assertThat(ex.getCause()).isNull();
        assertThat(ex.getDomain()).isEqualTo("ucp");
    }

    @Test
    void constructsWithFiveArguments() {
        UcpProtocolException ex = new UcpProtocolException(404, "not_found", "Item not found", "unrecoverable", "$.id");

        assertThat(ex.getStatusCode()).isEqualTo(404);
        assertThat(ex.getErrorCode()).isEqualTo("not_found");
        assertThat(ex.getMessage()).isEqualTo("Item not found");
        assertThat(ex.getSeverity()).isEqualTo("unrecoverable");
        assertThat(ex.getPath()).isEqualTo("$.id");
        assertThat(ex.getCause()).isNull();
        assertThat(ex.getDomain()).isEqualTo("ucp");
    }

    @Test
    void constructsWithCause() {
        IllegalStateException cause = new IllegalStateException("root cause");
        UcpProtocolException ex = new UcpProtocolException(
                500, "internal_error", "Something went wrong", "unrecoverable", "$.body", cause);

        assertThat(ex.getStatusCode()).isEqualTo(500);
        assertThat(ex.getErrorCode()).isEqualTo("internal_error");
        assertThat(ex.getMessage()).isEqualTo("Something went wrong");
        assertThat(ex.getSeverity()).isEqualTo("unrecoverable");
        assertThat(ex.getPath()).isEqualTo("$.body");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getDomain()).isEqualTo("ucp");
    }
}
