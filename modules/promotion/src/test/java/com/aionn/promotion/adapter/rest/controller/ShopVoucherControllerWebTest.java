package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.exception.PromotionExceptionHandler;
import com.aionn.promotion.adapter.rest.mapper.voucher.VoucherDtoMapperImpl;
import com.aionn.promotion.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.promotion.application.dto.analytics.result.MerchantVoucherAnalyticsResult;
import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.dto.voucher.result.VoucherResult;
import com.aionn.promotion.application.port.in.analytics.GetMerchantVoucherAnalyticsInputPort;
import com.aionn.promotion.application.port.in.voucher.IssueShopVoucherInputPort;
import com.aionn.promotion.application.port.in.voucher.ListMyShopVouchersInputPort;
import com.aionn.promotion.application.port.in.voucher.ListShopVouchersByMerchantInputPort;
import com.aionn.promotion.domain.valueobject.VoucherScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.aionn.sharedkernel.infrastructure.config.JacksonMapperFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShopVoucherControllerWebTest {

    private static final Instant NOW = Instant.parse("2026-06-25T00:00:00Z");

    @Mock
    private IssueShopVoucherInputPort issueShopVoucherInputPort;
    @Mock
    private ListMyShopVouchersInputPort listMyShopVouchersInputPort;
    @Mock
    private ListShopVouchersByMerchantInputPort listShopVouchersByMerchantInputPort;
    @Mock
    private GetMerchantVoucherAnalyticsInputPort getMerchantVoucherAnalyticsInputPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ShopVoucherController controller = new ShopVoucherController(
                issueShopVoucherInputPort, listMyShopVouchersInputPort,
                listShopVouchersByMerchantInputPort, getMerchantVoucherAnalyticsInputPort,
                new VoucherDtoMapperImpl());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PromotionExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(
                        JacksonMapperFactory.create()))
                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
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

    private static VoucherResult sample(String code) {
        return new VoucherResult(code, null, VoucherScope.SHOP, "mer-1",
                new BigDecimal("30000"), "VND", 20, 0, 0,
                NOW, NOW.plusSeconds(86400), NOW, NOW);
    }

    @Test
    void analyticsReturnsMerchantSummary() throws Exception {
        when(getMerchantVoucherAnalyticsInputPort.execute("owner-1")).thenReturn(
                new MerchantVoucherAnalyticsResult(150, 25, 125, 0.1667,
                        new BigDecimal("300000"), List.of()));

        mockMvc.perform(get("/api/v1/promotions/shop-vouchers/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalIssued").value(150))
                .andExpect(jsonPath("$.data.totalRedeemed").value(25));
    }

    @Test
    void issueUppercasesNothingButForwardsOwnerId() throws Exception {
        when(issueShopVoucherInputPort.execute(any(VoucherCommands.IssueShopVoucher.class)))
                .thenReturn(sample("SHOP10"));

        mockMvc.perform(post("/api/v1/promotions/shop-vouchers")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "voucherCode": "SHOP10",
                          "discountAmount": 30000,
                          "currency": "VND",
                          "usageLimit": 20
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.voucherCode").value("SHOP10"))
                .andExpect(jsonPath("$.data.scope").value("SHOP"));

        ArgumentCaptor<VoucherCommands.IssueShopVoucher> captor = ArgumentCaptor
                .forClass(VoucherCommands.IssueShopVoucher.class);
        verify(issueShopVoucherInputPort).execute(captor.capture());
        assertThat(captor.getValue().ownerId()).isEqualTo("owner-1");
    }

    @Test
    void issueRejectsMissingVoucherCode() throws Exception {
        mockMvc.perform(post("/api/v1/promotions/shop-vouchers")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "voucherCode": "",
                          "discountAmount": 30000,
                          "currency": "VND",
                          "usageLimit": 20
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listMineUsesDefaultLimit() throws Exception {
        when(listMyShopVouchersInputPort.execute("owner-1", 50))
                .thenReturn(List.of(sample("SHOP10")));

        mockMvc.perform(get("/api/v1/promotions/shop-vouchers/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].voucherCode").value("SHOP10"));
    }

    @Test
    void listByMerchantUsesDefaultLimit() throws Exception {
        when(listShopVouchersByMerchantInputPort.execute("mer-1", 20))
                .thenReturn(List.of(sample("SHOP10"), sample("SHOP20")));

        mockMvc.perform(get("/api/v1/promotions/shop-vouchers/merchant/mer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].voucherCode").value("SHOP20"));
    }
}
