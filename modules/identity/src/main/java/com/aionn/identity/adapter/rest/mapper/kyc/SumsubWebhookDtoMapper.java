package com.aionn.identity.adapter.rest.mapper.kyc;

import com.aionn.identity.application.dto.kyc.command.SumsubWebhookCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Translates the Sumsub callback body and digest headers into the webhook command. Provider-specific
 * field names stay in the adapter, not in the application layer.
 */
@Component
@RequiredArgsConstructor
public class SumsubWebhookDtoMapper {

    private final ObjectMapper objectMapper;

    public SumsubWebhookCommand toCommand(byte[] rawBody, String payloadDigest, String payloadDigestAlg)
            throws IOException {
        JsonNode payload = objectMapper.readTree(rawBody);
        JsonNode reviewResult = payload.path("reviewResult");
        return new SumsubWebhookCommand(
                rawBody,
                payloadDigest,
                payloadDigestAlg,
                text(payload, "applicantId"),
                text(payload, "reviewStatus"),
                text(reviewResult, "reviewAnswer"),
                text(reviewResult, "moderationComment"),
                text(reviewResult, "clientComment"),
                text(payload, "correlationId"));
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
