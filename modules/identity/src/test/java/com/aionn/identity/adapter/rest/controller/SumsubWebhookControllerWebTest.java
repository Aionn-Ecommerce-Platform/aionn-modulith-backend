package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.application.dto.kyc.command.SumsubWebhookCommand;
import com.aionn.identity.application.port.in.kyc.HandleSumsubWebhookInputPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SumsubWebhookControllerWebTest {

    @Mock
    private HandleSumsubWebhookInputPort handleSumsubWebhookInputPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SumsubWebhookController controller = new SumsubWebhookController(
                new ObjectMapper(), handleSumsubWebhookInputPort);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IdentityExceptionHandler())
                .build();
    }

    @Test
    void handleSumsubWebhookExtractsFieldsFromPayload() throws Exception {
        String payload = """
                {
                  "applicantId": "app-123",
                  "reviewStatus": "completed",
                  "correlationId": "corr-abc",
                  "reviewResult": {
                    "reviewAnswer": "GREEN",
                    "moderationComment": "ok",
                    "clientComment": "verified"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/kyc/webhooks/sumsub")
                        .header("X-Payload-Digest", "digest-hex")
                        .header("X-Payload-Digest-Alg", "HMAC_SHA256_HEX")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sumsub webhook processed"));

        ArgumentCaptor<SumsubWebhookCommand> captor = ArgumentCaptor.forClass(SumsubWebhookCommand.class);
        verify(handleSumsubWebhookInputPort).execute(captor.capture());
        SumsubWebhookCommand cmd = captor.getValue();
        assertThat(cmd.digest()).isEqualTo("digest-hex");
        assertThat(cmd.digestAlgorithm()).isEqualTo("HMAC_SHA256_HEX");
        assertThat(cmd.providerApplicantId()).isEqualTo("app-123");
        assertThat(cmd.providerReviewStatus()).isEqualTo("completed");
        assertThat(cmd.reviewAnswer()).isEqualTo("GREEN");
        assertThat(cmd.moderationComment()).isEqualTo("ok");
        assertThat(cmd.clientComment()).isEqualTo("verified");
        assertThat(cmd.correlationId()).isEqualTo("corr-abc");
    }

    @Test
    void handleSumsubWebhookAcceptsPayloadWithoutReviewResult() throws Exception {
        String payload = """
                { "applicantId": "app-1", "reviewStatus": "pending" }
                """;

        mockMvc.perform(post("/api/v1/kyc/webhooks/sumsub")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        ArgumentCaptor<SumsubWebhookCommand> captor = ArgumentCaptor.forClass(SumsubWebhookCommand.class);
        verify(handleSumsubWebhookInputPort).execute(captor.capture());
        SumsubWebhookCommand cmd = captor.getValue();
        assertThat(cmd.reviewAnswer()).isNull();
        assertThat(cmd.moderationComment()).isNull();
        assertThat(cmd.correlationId()).isNull();
    }
}
