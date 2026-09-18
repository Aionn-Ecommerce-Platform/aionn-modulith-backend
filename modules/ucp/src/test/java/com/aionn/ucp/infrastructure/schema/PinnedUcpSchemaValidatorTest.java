package com.aionn.ucp.infrastructure.schema;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aionn.ucp.adapter.rest.dto.UcpErrorResponse;
import com.aionn.ucp.adapter.rest.dto.UcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PinnedUcpSchemaValidatorTest {

        private final ObjectMapper mapper = new ObjectMapper();
        private PinnedUcpSchemaValidator validator;

        @BeforeEach
        void setUp() {
                validator = new PinnedUcpSchemaValidator();
        }

        @Test
        void validatesValidErrorResponse() {
                UcpErrorResponse error = UcpErrorResponse.of(
                                "2026-08-25",
                                "not_found",
                                "Resource not found",
                                UcpMessage.SEVERITY_UNRECOVERABLE,
                                "$.id");

                JsonNode jsonNode = mapper.valueToTree(error);
                assertThatCode(() -> validator.validateErrorResponse(jsonNode))
                                .doesNotThrowAnyException();
        }

        @Test
        void validatesReleasedErrorScaffold() throws Exception {
                try (InputStream is = getClass().getResourceAsStream(
                                "/ucp-contract/2026-08-25/scaffolds/shopping_types_error_response_response.json")) {
                        JsonNode scaffold = mapper.readTree(is);
                        assertThatCode(() -> validator.validateErrorResponse(scaffold))
                                        .doesNotThrowAnyException();
                }
        }

        @Test
        void rejectsErrorResponseWithMissingMessages() {
                ObjectNode error = mapper.createObjectNode();
                ObjectNode ucp = error.putObject("ucp");
                ucp.put("version", "2026-08-25");
                ucp.put("status", "error");
                // Missing "messages" array

                assertThatThrownBy(() -> validator.validateErrorResponse(error))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("failed validation");
        }

        @Test
        void rejectsErrorResponseWithWrongStatus() {
                UcpErrorResponse error = new UcpErrorResponse(
                                new UcpErrorResponse.Metadata("2026-08-25", "success"), // Invalid status for error
                                                                                        // response
                                java.util.List.of(UcpMessage.error("code", "msg", "unrecoverable")),
                                null);

                JsonNode jsonNode = mapper.valueToTree(error);
                assertThatThrownBy(() -> validator.validateErrorResponse(jsonNode))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("failed validation");
        }

        @Test
        void rejectsErrorResponseWithInvalidContinueUrl() {
                UcpErrorResponse error = new UcpErrorResponse(
                                new UcpErrorResponse.Metadata("2026-08-25", "error"),
                                java.util.List.of(UcpMessage.error("code", "msg", "unrecoverable")),
                                "not-a-valid-uri");

                JsonNode jsonNode = mapper.valueToTree(error);
                assertThatThrownBy(() -> validator.validateErrorResponse(jsonNode))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("failed validation");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                        "https://evil.com/schema.json",
                        "https://ucp.dev/schemas/../secret.json",
                        "file:///etc/passwd",
                        "classpath:something.json"
        })
        void rejectsUntrustedOrTraversalUris(String uri) {
                assertThatThrownBy(() -> validator.loadSchema(uri))
                                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsNullPayload() {
                assertThatThrownBy(() -> validator.validate("https://ucp.dev/schemas/common/types/error_response.json",
                                null))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Payload cannot be null");
        }

        @Test
        void defaultConstructorInitializesSuccessfully() {
                PinnedUcpSchemaValidator defaultValidator = new PinnedUcpSchemaValidator();
                UcpErrorResponse error = UcpErrorResponse.of("2026-08-25", "err", "msg", "recoverable");
                JsonNode jsonNode = mapper.valueToTree(error);
                org.junit.jupiter.api.Assertions
                                .assertDoesNotThrow(() -> defaultValidator.validateErrorResponse(jsonNode));
        }

        @Test
        void rejectsNonExistentSchemaUnderValidPrefix() {
                assertThatThrownBy(() -> validator.loadSchema("https://ucp.dev/schemas/non_existent_schema.json"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("not in the pinned contract registry");
        }
}
