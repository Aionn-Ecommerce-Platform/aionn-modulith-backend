package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.application.service.VnpayWebhookService;
import com.aionn.payment.domain.valueobject.VnpayResponseCode;
import com.aionn.payment.infrastructure.provider.config.VnpayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VnpayReturnControllerWebTest {

    @Mock
    private VnpayWebhookService vnpayWebhookService;
    @Mock
    private VnpayProperties vnpayProperties;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new VnpayReturnController(vnpayWebhookService, vnpayProperties)).build();
    }

    @Test
    void redirectsWithEncodedPaymentAndOrderIds() throws Exception {
        when(vnpayProperties.frontendReturnUrl()).thenReturn("http://fe/return");
        when(vnpayWebhookService.handleReturn(any())).thenReturn(new VnpayWebhookService.ReturnResult(
                VnpayWebhookService.ReturnStatus.SUCCESS, "pay/1 &", "order?1=ok"));

        mockMvc.perform(get("/api/v1/payments/vnpay/return?vnpay_params=1"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(
                        "http://fe/return?paymentId=pay%2F1%20%26&orderId=order%3F1%3Dok"));
    }

    @Test
    void returnsBadRequestWhenReturnIsRejected() throws Exception {
        when(vnpayWebhookService.handleReturn(any())).thenReturn(new VnpayWebhookService.ReturnResult(
                VnpayWebhookService.ReturnStatus.BAD_REQUEST, null, null));

        mockMvc.perform(get("/api/v1/payments/vnpay/return?invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsInternalErrorWhenReturnFinalisationFails() throws Exception {
        when(vnpayWebhookService.handleReturn(any())).thenReturn(new VnpayWebhookService.ReturnResult(
                VnpayWebhookService.ReturnStatus.INTERNAL_ERROR, "pay-1", null));

        mockMvc.perform(get("/api/v1/payments/vnpay/return?valid"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void returnsIpnResult() throws Exception {
        when(vnpayWebhookService.handleIpn(any())).thenReturn(
                new VnpayWebhookService.IpnResult(VnpayResponseCode.CONFIRM_SUCCESS));

        mockMvc.perform(post("/api/v1/payments/vnpay/ipn?vnpay_params=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));
    }
}
