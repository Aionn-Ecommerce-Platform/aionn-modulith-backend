package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.exception.PromotionExceptionHandler;
import com.aionn.promotion.adapter.rest.support.session.CurrentAdminIdArgumentResolver;
import com.aionn.promotion.application.dto.campaign.command.CampaignCommands;
import com.aionn.promotion.application.dto.campaign.result.CampaignResult;
import com.aionn.promotion.application.dto.voucher.result.VoucherResult;
import com.aionn.promotion.application.service.PromotionCampaignService;
import com.aionn.promotion.domain.valueobject.VoucherScope;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PromotionCampaignControllerWebTest {

    @Mock
    private PromotionCampaignService campaignService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PromotionCampaignController controller = new PromotionCampaignController(campaignService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PromotionExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .setCustomArgumentResolvers(new CurrentAdminIdArgumentResolver())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin-1", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static CampaignResult sample(String id, String status) {
        Instant now = Instant.now();
        return new CampaignResult(id, "Summer Sale", "DISCOUNT",
                new BigDecimal("1000000"), new BigDecimal("1000000"), "VND",
                now, now.plusSeconds(86400), "admin-1", status,
                null, List.of(), null, null, now, now);
    }

    private static VoucherResult voucherSample(String code) {
        Instant now = Instant.now();
        return new VoucherResult(code, "camp-1", VoucherScope.PLATFORM, null,
                new BigDecimal("50000"), "VND", 100, 0, 0,
                now, now.plusSeconds(86400), now, now);
    }

    @Test
    void createReturnsCreatedCampaign() throws Exception {
        when(campaignService.create(any(CampaignCommands.CreateCampaign.class)))
                .thenReturn(sample("camp-1", "DRAFT"));

        mockMvc.perform(post("/api/v1/promotions/campaigns")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Summer Sale",
                                  "type": "DISCOUNT",
                                  "budget": 1000000,
                                  "currency": "VND",
                                  "startDate": "2026-06-25T00:00:00Z",
                                  "endDate": "2026-06-26T00:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.campaignId").value("camp-1"))
                .andExpect(jsonPath("$.message").value("Campaign created"));

        verify(campaignService).create(any(CampaignCommands.CreateCampaign.class));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/promotions/campaigns")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "type": "DISCOUNT",
                                  "budget": 1000000,
                                  "currency": "VND",
                                  "startDate": "2026-06-25T00:00:00Z",
                                  "endDate": "2026-06-26T00:00:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activateInvokesService() throws Exception {
        when(campaignService.activate(any(CampaignCommands.ActivateCampaign.class)))
                .thenReturn(sample("camp-1", "RUNNING"));

        mockMvc.perform(post("/api/v1/promotions/campaigns/camp-1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    void cancelInvokesServiceWithReason() throws Exception {
        when(campaignService.cancel(any(CampaignCommands.CancelCampaign.class)))
                .thenReturn(sample("camp-1", "CANCELLED"));

        mockMvc.perform(post("/api/v1/promotions/campaigns/camp-1/cancel")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "wrong dates"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void configureConditionsUpdatesCampaign() throws Exception {
        when(campaignService.configureCondition(any(CampaignCommands.ConfigureCondition.class)))
                .thenReturn(sample("camp-1", "DRAFT"));

        mockMvc.perform(put("/api/v1/promotions/campaigns/camp-1/conditions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "minOrderValue": 100000,
                                  "applicableCategoryIds": ["cat-1"],
                                  "maxClaimsPerUser": 1,
                                  "maxUsesPerVoucher": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Conditions updated"));
    }

    @Test
    void issueVoucherInvokesService() throws Exception {
        when(campaignService.issueVoucher(any(CampaignCommands.IssueVoucher.class)))
                .thenReturn(voucherSample("V-100"));

        mockMvc.perform(post("/api/v1/promotions/campaigns/camp-1/vouchers")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "voucherCode": "V-100",
                                  "discountAmount": 50000,
                                  "currency": "VND",
                                  "usageLimit": 100
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.voucherCode").value("V-100"));
    }

    @Test
    void getReturnsCampaign() throws Exception {
        when(campaignService.get("camp-1")).thenReturn(sample("camp-1", "DRAFT"));

        mockMvc.perform(get("/api/v1/promotions/campaigns/camp-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.campaignId").value("camp-1"));
    }

    @Test
    void listByStatusReturnsCampaigns() throws Exception {
        when(campaignService.listByStatus("RUNNING", 50))
                .thenReturn(List.of(sample("camp-1", "RUNNING"), sample("camp-2", "RUNNING")));

        mockMvc.perform(get("/api/v1/promotions/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].campaignId").value("camp-1"))
                .andExpect(jsonPath("$.data[1].campaignId").value("camp-2"));
    }
}
