package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.application.dto.kyc.command.SumsubWebhookCommand;
import com.aionn.identity.application.port.in.kyc.HandleSumsubWebhookInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/api/v1/kyc/webhooks")
@RequiredArgsConstructor
public class SumsubWebhookController {

    private final ObjectMapper objectMapper;
    private final HandleSumsubWebhookInputPort handleSumsubWebhookInputPort;

    @PostMapping("/sumsub")
    public ResponseEntity<ApiResponse<Void>> handleSumsubWebhook(
            @RequestHeader(name = "X-Payload-Digest", required = false) String payloadDigest,
            @RequestHeader(name = "X-Payload-Digest-Alg", required = false) String payloadDigestAlg,
            @RequestBody byte[] rawBody) throws Exception {
        JsonNode payload = objectMapper.readTree(rawBody);
        JsonNode reviewResult = payload.path("reviewResult");

        handleSumsubWebhookInputPort.execute(new SumsubWebhookCommand(
                rawBody,
                payloadDigest,
                payloadDigestAlg,
                text(payload, "applicantId"),
                text(payload, "reviewStatus"),
                text(reviewResult, "reviewAnswer"),
                text(reviewResult, "moderationComment"),
                text(reviewResult, "clientComment"),
                text(payload, "correlationId")));

        return ResponseEntity.ok(ApiResponse.success("Sumsub webhook processed"));
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
