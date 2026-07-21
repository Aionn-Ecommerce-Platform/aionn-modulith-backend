package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.mapper.payment.PaymentDtoMapper;
import com.aionn.payment.application.dto.payment.command.ConfirmPaymentCommand;
import com.aionn.payment.application.dto.payment.command.FailPaymentCommand;
import com.aionn.payment.application.port.in.payment.ConfirmPaymentInputPort;
import com.aionn.payment.application.port.in.payment.FailPaymentInputPort;
import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.application.port.out.PaymentProviderRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentWebhookControllerWebTest {

        private PaymentProviderRouter providerRouter;
        private PaymentProviderClient providerClient;

        @Mock
        private ConfirmPaymentInputPort confirmPaymentInputPort;
        @Mock
        private FailPaymentInputPort failPaymentInputPort;
        @Mock
        private PaymentDtoMapper paymentDtoMapper;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
                providerRouter = org.mockito.Mockito.mock(PaymentProviderRouter.class);
                providerClient = org.mockito.Mockito.mock(PaymentProviderClient.class);

                org.mockito.Mockito.lenient().when(providerRouter.route(any())).thenReturn(providerClient);

                // Mock mapper methods to return commands
                org.mockito.Mockito.lenient().when(paymentDtoMapper.toConfirmCommand(any()))
                                .thenAnswer(inv -> {
                                        PaymentProviderClient.WebhookEvent event = inv.getArgument(0);
                                        return new ConfirmPaymentCommand(event.paymentId(), event.transactionNo());
                                });

                org.mockito.Mockito.lenient().when(paymentDtoMapper.toFailCommand(any(), any()))
                                .thenAnswer(inv -> {
                                        PaymentProviderClient.WebhookEvent event = inv.getArgument(0);
                                        String defaultCode = inv.getArgument(1);
                                        String errorCode = event.errorCode() != null ? event.errorCode() : defaultCode;
                                        return new FailPaymentCommand(event.paymentId(), errorCode,
                                                        event.errorReason());
                                });

                PaymentWebhookController controller = new PaymentWebhookController(
                                providerRouter, confirmPaymentInputPort, failPaymentInputPort, paymentDtoMapper);

                mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        }

        @Test
        void handleSuccessfulEventConfirmsPayment() throws Exception {
                PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                                "payment.captured", "pay-1", "txn-1",
                                new BigDecimal("100"), "VND", true, null, null);
                when(providerClient.verifyAndParse(any(), any())).thenReturn(event);

                mockMvc.perform(post("/api/v1/payments/webhooks/stripe")
                                .header("X-Signature", "sig")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"raw\":true}"))
                                .andExpect(status().is2xxSuccessful());

                verify(confirmPaymentInputPort).execute(any(ConfirmPaymentCommand.class));
                verify(failPaymentInputPort, never()).execute(any());
        }

        @Test
        void handleFailedEventFailsPayment() throws Exception {
                PaymentProviderClient.WebhookEvent event = new PaymentProviderClient.WebhookEvent(
                                "payment.failed", "pay-1", "txn-1",
                                new BigDecimal("100"), "VND", false, "declined", "card declined");
                when(providerClient.verifyAndParse(any(), any())).thenReturn(event);

                mockMvc.perform(post("/api/v1/payments/webhooks/stripe")
                                .header("X-Signature", "sig")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"raw\":true}"))
                                .andExpect(status().is2xxSuccessful());

                verify(failPaymentInputPort).execute(any(FailPaymentCommand.class));
                verify(confirmPaymentInputPort, never()).execute(any());
        }

        @Test
        void webhookWithoutPaymentIdReturnsBadRequest() throws Exception {
                org.mockito.Mockito.lenient().when(providerClient.verifyAndParse(any(), any())).thenReturn(
                                new PaymentProviderClient.WebhookEvent("ping", null, null, null, null, true, null,
                                                null));

                mockMvc.perform(post("/api/v1/payments/webhooks/stripe")
                                .header("X-Signature", "sig")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"raw\":true}"))
                                .andExpect(status().isBadRequest());

                verify(confirmPaymentInputPort, never()).execute(any());
                verify(failPaymentInputPort, never()).execute(any());
        }

        @Test
        void handleUnknownGatewayReturnsBadRequest() throws Exception {
                mockMvc.perform(post("/api/v1/payments/webhooks/unknown-gateway")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"raw\":true}"))
                                .andExpect(status().isBadRequest());
        }
}
