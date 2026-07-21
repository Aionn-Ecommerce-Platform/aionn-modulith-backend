package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.dto.payout.response.PayoutResponse;
import com.aionn.payment.adapter.rest.exception.PaymentExceptionHandler;
import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;
import com.aionn.payment.application.port.in.payout.CompletePayoutInputPort;
import com.aionn.payment.application.port.in.payout.FailPayoutInputPort;
import com.aionn.payment.application.port.in.payout.ListPayoutsByStatusInputPort;
import com.aionn.payment.adapter.rest.mapper.payout.PayoutDtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminPayoutControllerWebTest {

    @Mock
    private ListPayoutsByStatusInputPort listPayoutsByStatusInputPort;
    @Mock
    private CompletePayoutInputPort completePayoutInputPort;
    @Mock
    private FailPayoutInputPort failPayoutInputPort;
    @Mock
    private PayoutDtoMapper payoutDtoMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminPayoutController controller = new AdminPayoutController(
                listPayoutsByStatusInputPort,
                completePayoutInputPort,
                failPayoutInputPort,
                payoutDtoMapper
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PaymentExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .build();
    }

    @Test
    void shouldListPayoutsByStatus() throws Exception {
        MerchantPayoutResult result = new MerchantPayoutResult("p-1", "m-1", new BigDecimal("100.00"), "VND", "PENDING", "VCB", "123", "Name", null, null, Instant.now(), null, null, null);
        PayoutResponse response = new PayoutResponse("p-1", "m-1", new BigDecimal("100.00"), "VND", "PENDING", "VCB", "123", "Name", null, null, Instant.now(), null, null, null);

        when(listPayoutsByStatusInputPort.execute("PENDING", 100)).thenReturn(List.of(result));
        when(payoutDtoMapper.toResponses(List.of(result))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/admin/payouts")
                        .param("status", "PENDING")
                        .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].payoutId").value("p-1"));
    }

    @Test
    void shouldMarkPayoutComplete() throws Exception {
        MerchantPayoutResult result = new MerchantPayoutResult("p-1", "m-1", new BigDecimal("100.00"), "VND", "COMPLETED", "VCB", "123", "Name", "external-123", null, Instant.now(), Instant.now(), null, null);
        PayoutResponse response = new PayoutResponse("p-1", "m-1", new BigDecimal("100.00"), "VND", "COMPLETED", "VCB", "123", "Name", "external-123", null, Instant.now(), Instant.now(), null, null);

        when(completePayoutInputPort.execute("p-1", "external-123")).thenReturn(result);
        when(payoutDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/payouts/p-1/complete")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "externalRef": "external-123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.externalRef").value("external-123"));
    }

    @Test
    void shouldMarkPayoutFailed() throws Exception {
        MerchantPayoutResult result = new MerchantPayoutResult("p-1", "m-1", new BigDecimal("100.00"), "VND", "FAILED", "VCB", "123", "Name", null, null, Instant.now(), null, Instant.now(), "Rejected by Bank");
        PayoutResponse response = new PayoutResponse("p-1", "m-1", new BigDecimal("100.00"), "VND", "FAILED", "VCB", "123", "Name", null, null, Instant.now(), null, Instant.now(), "Rejected by Bank");

        when(failPayoutInputPort.execute("p-1", "Rejected by Bank")).thenReturn(result);
        when(payoutDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/payouts/p-1/fail")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Rejected by Bank"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.failureReason").value("Rejected by Bank"));
    }
}
