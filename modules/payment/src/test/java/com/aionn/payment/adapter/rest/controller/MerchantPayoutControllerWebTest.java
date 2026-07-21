package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.dto.payout.response.MerchantBalanceResponse;
import com.aionn.payment.adapter.rest.dto.payout.response.PayoutResponse;
import com.aionn.payment.adapter.rest.exception.PaymentExceptionHandler;
import com.aionn.payment.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.payment.application.dto.payout.result.MerchantBalanceResult;
import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;
import com.aionn.payment.application.port.in.payout.GetMerchantBalanceInputPort;
import com.aionn.payment.application.port.in.payout.ListMerchantPayoutsInputPort;
import com.aionn.payment.application.port.in.payout.RequestPayoutInputPort;
import com.aionn.payment.adapter.rest.mapper.payout.PayoutDtoMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantPayoutControllerWebTest {

    @Mock
    private GetMerchantBalanceInputPort getMerchantBalanceInputPort;
    @Mock
    private ListMerchantPayoutsInputPort listMerchantPayoutsInputPort;
    @Mock
    private RequestPayoutInputPort requestPayoutInputPort;
    @Mock
    private PayoutDtoMapper payoutDtoMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MerchantPayoutController controller = new MerchantPayoutController(
                getMerchantBalanceInputPort,
                listMerchantPayoutsInputPort,
                requestPayoutInputPort,
                payoutDtoMapper
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PaymentExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "merchant-owner", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldGetBalance() throws Exception {
        MerchantBalanceResult result = new MerchantBalanceResult("m-1", "VND", BigDecimal.ZERO, new BigDecimal("500.00"), Instant.now());
        MerchantBalanceResponse response = new MerchantBalanceResponse("m-1", "VND", BigDecimal.ZERO, new BigDecimal("500.00"), Instant.now());

        when(getMerchantBalanceInputPort.execute("merchant-owner", "VND")).thenReturn(result);
        when(payoutDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/merchant/balance")
                        .param("currency", "VND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchantId").value("m-1"))
                .andExpect(jsonPath("$.data.available").value(500.00));
    }

    @Test
    void shouldListPayouts() throws Exception {
        MerchantPayoutResult result = new MerchantPayoutResult("p-1", "m-1", new BigDecimal("100.00"), "VND", "PENDING", "VCB", "123", "Name", null, null, Instant.now(), null, null, null);
        PayoutResponse response = new PayoutResponse("p-1", "m-1", new BigDecimal("100.00"), "VND", "PENDING", "VCB", "123", "Name", null, null, Instant.now(), null, null, null);

        when(listMerchantPayoutsInputPort.execute("merchant-owner", 50)).thenReturn(List.of(result));
        when(payoutDtoMapper.toResponses(List.of(result))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/payments/merchant/payouts")
                        .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].payoutId").value("p-1"));
    }

    @Test
    void shouldRequestPayout() throws Exception {
        MerchantPayoutResult result = new MerchantPayoutResult("p-1", "m-1", new BigDecimal("100.00"), "VND", "PENDING", "VCB", "123", "Name", null, null, Instant.now(), null, null, null);
        PayoutResponse response = new PayoutResponse("p-1", "m-1", new BigDecimal("100.00"), "VND", "PENDING", "VCB", "123", "Name", null, null, Instant.now(), null, null, null);

        com.aionn.payment.application.dto.payout.command.RequestPayoutCommand command = new com.aionn.payment.application.dto.payout.command.RequestPayoutCommand("merchant-owner", new BigDecimal("100.00"), "VND", "VCB", "123", "Name", "Note");
        
        when(payoutDtoMapper.toCommand(any(), any())).thenReturn(command);
        when(requestPayoutInputPort.execute(command)).thenReturn(result);
        when(payoutDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/merchant/payouts")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 100.00,
                                  "currency": "VND",
                                  "bankName": "VCB",
                                  "bankAccountNo": "123",
                                  "bankAccountName": "Name",
                                  "note": "Note"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.payoutId").value("p-1"));
    }
}
