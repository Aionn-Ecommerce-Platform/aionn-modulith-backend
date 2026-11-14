package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.application.service.PaymentWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookControllerWebTest {

    @Mock
    private PaymentWebhookService paymentWebhookService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new PaymentWebhookController(paymentWebhookService)).build();
    }

    @Test
    void handleAcceptedWebhookReturnsOk() throws Exception {
        when(paymentWebhookService.handle(any(), any(), any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/payments/webhooks/stripe")
                        .header("X-Signature", "sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"raw\":true}"))
                .andExpect(status().isOk());

        verify(paymentWebhookService).handle(any(), any(), any());
    }

    @Test
    void handleRejectedWebhookReturnsBadRequest() throws Exception {
        when(paymentWebhookService.handle(any(), any(), any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/payments/webhooks/stripe")
                        .header("X-Signature", "sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"raw\":true}"))
                .andExpect(status().isBadRequest());
    }
}
