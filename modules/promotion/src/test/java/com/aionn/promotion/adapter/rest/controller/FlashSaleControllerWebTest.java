package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.exception.PromotionExceptionHandler;
import com.aionn.promotion.adapter.rest.mapper.flashsale.FlashSaleDtoMapperImpl;
import com.aionn.promotion.adapter.rest.support.session.CurrentAdminIdArgumentResolver;
import com.aionn.promotion.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.promotion.application.dto.flashsale.command.FlashSaleCommands;
import com.aionn.promotion.application.dto.flashsale.result.ActiveFlashSaleResult;
import com.aionn.promotion.application.dto.flashsale.result.FlashSaleRegistrationResult;
import com.aionn.promotion.application.port.in.flashsale.ApproveFlashSaleInputPort;
import com.aionn.promotion.application.port.in.flashsale.CancelFlashSaleInputPort;
import com.aionn.promotion.application.port.in.flashsale.GetFlashSaleRegistrationInputPort;
import com.aionn.promotion.application.port.in.flashsale.ListActiveFlashSalesInputPort;
import com.aionn.promotion.application.port.in.flashsale.ListFlashSaleRegistrationsByStatusInputPort;
import com.aionn.promotion.application.port.in.flashsale.ListMyFlashSaleRegistrationsInputPort;
import com.aionn.promotion.application.port.in.flashsale.RegisterFlashSaleInputPort;
import com.aionn.promotion.application.port.in.flashsale.RejectFlashSaleInputPort;
import com.aionn.promotion.domain.valueobject.FlashSaleRegistrationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FlashSaleControllerWebTest {

    private static final Instant NOW = Instant.parse("2026-06-25T00:00:00Z");

    @Mock
    private RegisterFlashSaleInputPort registerFlashSaleInputPort;
    @Mock
    private ApproveFlashSaleInputPort approveFlashSaleInputPort;
    @Mock
    private RejectFlashSaleInputPort rejectFlashSaleInputPort;
    @Mock
    private CancelFlashSaleInputPort cancelFlashSaleInputPort;
    @Mock
    private ListMyFlashSaleRegistrationsInputPort listMyFlashSaleRegistrationsInputPort;
    @Mock
    private ListFlashSaleRegistrationsByStatusInputPort listFlashSaleRegistrationsByStatusInputPort;
    @Mock
    private GetFlashSaleRegistrationInputPort getFlashSaleRegistrationInputPort;
    @Mock
    private ListActiveFlashSalesInputPort listActiveFlashSalesInputPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FlashSaleController controller = new FlashSaleController(
                registerFlashSaleInputPort, approveFlashSaleInputPort, rejectFlashSaleInputPort,
                cancelFlashSaleInputPort, listMyFlashSaleRegistrationsInputPort,
                listFlashSaleRegistrationsByStatusInputPort, getFlashSaleRegistrationInputPort,
                listActiveFlashSalesInputPort, new FlashSaleDtoMapperImpl());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PromotionExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(
                        JsonMapper.builder().build()))
                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver(),
                        new CurrentAdminIdArgumentResolver())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "owner-1", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static FlashSaleRegistrationResult sample(String id, String status) {
        return new FlashSaleRegistrationResult(id, "camp-1", "mer-1", "prod-1", "sku-1",
                new BigDecimal("80000"), "VND", 10, 0, status, null,
                NOW, null, null, NOW);
    }

    @Test
    void registerReturnsCreatedRegistration() throws Exception {
        when(registerFlashSaleInputPort.execute(any(FlashSaleCommands.RegisterFlashSale.class)))
                .thenReturn(sample("reg-1", "PENDING"));

        mockMvc.perform(post("/api/v1/promotions/flash-sales/registrations")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "campaignId": "camp-1",
                          "productId": "prod-1",
                          "skuId": "sku-1",
                          "salePrice": 80000,
                          "currency": "VND",
                          "saleStock": 10
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.registrationId").value("reg-1"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(registerFlashSaleInputPort).execute(any(FlashSaleCommands.RegisterFlashSale.class));
    }

    @Test
    void registerRejectsBlankCampaignId() throws Exception {
        mockMvc.perform(post("/api/v1/promotions/flash-sales/registrations")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "campaignId": "",
                          "productId": "prod-1",
                          "skuId": "sku-1",
                          "salePrice": 80000,
                          "currency": "VND",
                          "saleStock": 10
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveInvokesInputPort() throws Exception {
        when(approveFlashSaleInputPort.execute(any(FlashSaleCommands.ApproveFlashSale.class)))
                .thenReturn(sample("reg-1", "APPROVED"));

        mockMvc.perform(post("/api/v1/promotions/flash-sales/registrations/reg-1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void rejectPassesReason() throws Exception {
        when(rejectFlashSaleInputPort.execute(any(FlashSaleCommands.RejectFlashSale.class)))
                .thenReturn(sample("reg-1", "REJECTED"));

        mockMvc.perform(post("/api/v1/promotions/flash-sales/registrations/reg-1/reject")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "reason": "discount too small"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void rejectRequiresReason() throws Exception {
        mockMvc.perform(post("/api/v1/promotions/flash-sales/registrations/reg-1/reject")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "reason": ""
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelInvokesInputPort() throws Exception {
        when(cancelFlashSaleInputPort.execute(any(FlashSaleCommands.CancelFlashSale.class)))
                .thenReturn(sample("reg-1", "CANCELLED"));

        mockMvc.perform(delete("/api/v1/promotions/flash-sales/registrations/reg-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void listMineUsesDefaultLimit() throws Exception {
        when(listMyFlashSaleRegistrationsInputPort.execute("owner-1", null, 100))
                .thenReturn(List.of(sample("reg-1", "PENDING")));

        mockMvc.perform(get("/api/v1/promotions/flash-sales/registrations/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].registrationId").value("reg-1"));
    }

    @Test
    void listMinePassesStatusFilter() throws Exception {
        when(listMyFlashSaleRegistrationsInputPort.execute("owner-1",
                FlashSaleRegistrationStatus.APPROVED, 5))
                .thenReturn(List.of(sample("reg-1", "APPROVED")));

        mockMvc.perform(get("/api/v1/promotions/flash-sales/registrations/mine")
                .param("status", "APPROVED")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"));
    }

    @Test
    void listByStatusRequiresStatusParam() throws Exception {
        mockMvc.perform(get("/api/v1/promotions/flash-sales/registrations"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listByStatusReturnsRegistrations() throws Exception {
        when(listFlashSaleRegistrationsByStatusInputPort.execute(
                FlashSaleRegistrationStatus.PENDING, 100))
                .thenReturn(List.of(sample("reg-1", "PENDING"), sample("reg-2", "PENDING")));

        mockMvc.perform(get("/api/v1/promotions/flash-sales/registrations")
                .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].registrationId").value("reg-2"));
    }

    @Test
    void getReturnsRegistration() throws Exception {
        when(getFlashSaleRegistrationInputPort.execute("reg-1"))
                .thenReturn(sample("reg-1", "APPROVED"));

        mockMvc.perform(get("/api/v1/promotions/flash-sales/registrations/reg-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registrationId").value("reg-1"));
    }

    @Test
    void activeReturnsStorefrontPayload() throws Exception {
        when(listActiveFlashSalesInputPort.execute(5)).thenReturn(List.of(
                new ActiveFlashSaleResult("camp-1", "Flash Friday", NOW, NOW.plusSeconds(3600),
                        List.of(new ActiveFlashSaleResult.Item("reg-1", "prod-1", "sku-1", "mer-1",
                                new BigDecimal("80000"), "VND", 10, 2)))));

        mockMvc.perform(get("/api/v1/promotions/flash-sales/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].campaignId").value("camp-1"))
                .andExpect(jsonPath("$.data[0].items[0].skuId").value("sku-1"))
                .andExpect(jsonPath("$.data[0].items[0].soldCount").value(2));
    }
}
