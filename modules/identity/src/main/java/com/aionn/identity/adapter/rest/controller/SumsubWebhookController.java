package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.mapper.kyc.SumsubWebhookDtoMapper;
import com.aionn.identity.application.port.in.kyc.HandleSumsubWebhookInputPort;
import com.aionn.platform.web.response.ApiResponse;
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

    private final SumsubWebhookDtoMapper webhookDtoMapper;
    private final HandleSumsubWebhookInputPort handleSumsubWebhookInputPort;

    @PostMapping("/sumsub")
    public ResponseEntity<ApiResponse<Void>> handleSumsubWebhook(
            @RequestHeader(name = "X-Payload-Digest", required = false) String payloadDigest,
            @RequestHeader(name = "X-Payload-Digest-Alg", required = false) String payloadDigestAlg,
            @RequestBody byte[] rawBody) throws Exception {
        handleSumsubWebhookInputPort.execute(
                webhookDtoMapper.toCommand(rawBody, payloadDigest, payloadDigestAlg));

        return ResponseEntity.ok(ApiResponse.success("Sumsub webhook processed"));
    }
}
