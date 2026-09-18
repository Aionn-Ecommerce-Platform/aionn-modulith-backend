package com.aionn.ucp.adapter.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class UcpErrorResponseTest {

    @Test
    void ofWithFourArgumentsCreatesCanonicalError() {
        UcpErrorResponse response = UcpErrorResponse.of("2026-08-25", "invalid_req", "Invalid payload",
                "unrecoverable");

        assertThat(response.ucp().version()).isEqualTo("2026-08-25");
        assertThat(response.ucp().status()).isEqualTo(UcpErrorResponse.STATUS_ERROR);
        assertThat(response.continueUrl()).isNull();
        assertThat(response.messages()).hasSize(1);

        UcpMessage msg = response.messages().get(0);
        assertThat(msg.type()).isEqualTo("error");
        assertThat(msg.code()).isEqualTo("invalid_req");
        assertThat(msg.content()).isEqualTo("Invalid payload");
        assertThat(msg.severity()).isEqualTo("unrecoverable");
        assertThat(msg.path()).isNull();
        assertThat(msg.contentType()).isEqualTo("plain");
    }

    @Test
    void ofWithFiveArgumentsIncludesPath() {
        UcpErrorResponse response = UcpErrorResponse.of(
                "2026-08-25", "invalid_field", "Field is missing", "recoverable", "$.items[0]");

        assertThat(response.messages().get(0).path()).isEqualTo("$.items[0]");
    }

    @Test
    void ofWithMessagesList() {
        List<UcpMessage> messages = List.of(
                UcpMessage.error("code1", "msg1", UcpMessage.SEVERITY_RECOVERABLE),
                UcpMessage.error("code2", "msg2", UcpMessage.SEVERITY_REQUIRES_BUYER_INPUT, "$.field"));

        UcpErrorResponse response = UcpErrorResponse.of("2026-08-25", messages);

        assertThat(response.messages()).hasSize(2);
        assertThat(response.messages().get(0).severity()).isEqualTo("recoverable");
        assertThat(response.messages().get(1).severity()).isEqualTo("requires_buyer_input");
    }

    @Test
    void ofWithNullMessagesDefaultsToEmptyList() {
        UcpErrorResponse response = UcpErrorResponse.of("2026-08-25", (List<UcpMessage>) null);

        assertThat(response.messages()).isEmpty();
    }
}
