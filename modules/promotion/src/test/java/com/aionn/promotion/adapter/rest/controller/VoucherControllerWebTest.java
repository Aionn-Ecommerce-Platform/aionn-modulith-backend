package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.mapper.voucher.VoucherDtoMapper;

import org.mapstruct.factory.Mappers;

import com.aionn.promotion.adapter.rest.exception.PromotionExceptionHandler;
import com.aionn.promotion.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.dto.voucher.result.UserVoucherResult;
import com.aionn.promotion.application.dto.voucher.result.VoucherSummaryResult;
import com.aionn.promotion.application.port.in.voucher.ApplyVoucherInputPort;
import com.aionn.promotion.application.port.in.voucher.ClaimVoucherInputPort;
import com.aionn.promotion.application.port.in.voucher.GetMyVoucherInputPort;
import com.aionn.promotion.application.port.in.voucher.GetVoucherSummaryInputPort;
import com.aionn.promotion.application.port.in.voucher.GrantVoucherInputPort;
import com.aionn.promotion.application.port.in.voucher.ListMyVouchersInputPort;
import com.aionn.promotion.application.port.in.voucher.ReleaseVoucherInputPort;
import com.aionn.promotion.application.port.in.voucher.ReserveVoucherInputPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.aionn.platform.infrastructure.config.JacksonMapperFactory;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VoucherControllerWebTest {

        @Mock
        private ClaimVoucherInputPort claimVoucherInputPort;
        @Mock
        private ReserveVoucherInputPort reserveVoucherInputPort;
        @Mock
        private ApplyVoucherInputPort applyVoucherInputPort;
        @Mock
        private ReleaseVoucherInputPort releaseVoucherInputPort;
        @Mock
        private ListMyVouchersInputPort listMyVouchersInputPort;
        @Mock
        private GetMyVoucherInputPort getMyVoucherInputPort;
        @Mock
        private GrantVoucherInputPort grantVoucherInputPort;
        @Mock
        private GetVoucherSummaryInputPort getVoucherSummaryInputPort;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                VoucherController controller = new VoucherController(
                                claimVoucherInputPort, reserveVoucherInputPort, applyVoucherInputPort,
                                releaseVoucherInputPort, listMyVouchersInputPort, getMyVoucherInputPort,
                                grantVoucherInputPort, getVoucherSummaryInputPort,
                                Mappers.getMapper(VoucherDtoMapper.class));

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new PromotionExceptionHandler())
                                .setMessageConverters(new JacksonJsonHttpMessageConverter(
                                                JacksonMapperFactory.create()))
                                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
                                .build();

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                                "user-1", "n/a",
                                                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        private static UserVoucherResult sample(String code, String status) {
                Instant now = Instant.parse("2026-06-25T00:00:00Z");
                return new UserVoucherResult(
                                "uv-1", code, "user-1", status, null,
                                null, "VND",
                                now, null, null, null, null, now,
                                new BigDecimal("50000"), "VND", "PLATFORM", "ORDER_DISCOUNT",
                                now.plusSeconds(86400), null, 100, 0);
        }

        @Test
        void claimReturnsUserVoucher() throws Exception {
                when(claimVoucherInputPort.execute(any(VoucherCommands.ClaimVoucher.class)))
                                .thenReturn(sample("V-1", "CLAIMED"));

                mockMvc.perform(post("/api/v1/promotions/vouchers/V-1/claim"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.voucherCode").value("V-1"))
                                .andExpect(jsonPath("$.data.status").value("CLAIMED"))
                                .andExpect(jsonPath("$.data.voucherType").value("ORDER_DISCOUNT"));

                verify(claimVoucherInputPort).execute(any(VoucherCommands.ClaimVoucher.class));
        }

        @Test
        void reserveInvokesInputPort() throws Exception {
                when(reserveVoucherInputPort.execute(any(VoucherCommands.ReserveVoucher.class)))
                                .thenReturn(sample("V-1", "RESERVED"));

                mockMvc.perform(post("/api/v1/promotions/vouchers/V-1/reserve")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "orderId": "order-1",
                                                  "orderValue": 200000,
                                                  "currency": "VND"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("RESERVED"));
        }

        @Test
        void reserveRejectsMissingOrderId() throws Exception {
                mockMvc.perform(post("/api/v1/promotions/vouchers/V-1/reserve")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "orderId": "",
                                                  "orderValue": 100,
                                                  "currency": "VND"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void applyInvokesInputPort() throws Exception {
                when(applyVoucherInputPort.execute(any(VoucherCommands.ApplyVoucher.class)))
                                .thenReturn(sample("V-1", "APPLIED"));

                mockMvc.perform(post("/api/v1/promotions/vouchers/V-1/apply")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "orderId": "order-1",
                                                  "appliedAmount": 50000,
                                                  "currency": "VND"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("APPLIED"));
        }

        @Test
        void releaseInvokesInputPort() throws Exception {
                when(releaseVoucherInputPort.execute(any(VoucherCommands.ReleaseVoucher.class)))
                                .thenReturn(sample("V-1", "RELEASED"));

                mockMvc.perform(post("/api/v1/promotions/vouchers/V-1/release")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "orderId": "order-1",
                                                  "reason": "buyer cancelled"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("RELEASED"));
        }

        @Test
        void listMineReturnsMyVouchers() throws Exception {
                when(listMyVouchersInputPort.executePaged(org.mockito.ArgumentMatchers.eq("user-1"), any(),
                                org.mockito.ArgumentMatchers.anyInt(),
                                org.mockito.ArgumentMatchers.anyInt()))
                                .thenReturn(new com.aionn.promotion.application.dto.voucher.result.UserVoucherPageResult(
                                                List.of(sample("V-1", "CLAIMED"), sample("V-2", "RESERVED")), 2));

                mockMvc.perform(get("/api/v1/promotions/vouchers/me"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].voucherCode").value("V-1"))
                                .andExpect(jsonPath("$.data[1].voucherCode").value("V-2"));
        }

        @Test
        void listMineRejectsLimitAboveMaximum() throws Exception {
                mockMvc.perform(get("/api/v1/promotions/vouchers/me").param("limit", "999"))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(listMyVouchersInputPort);
        }

        @Test
        void getMineReturnsVoucher() throws Exception {
                when(getMyVoucherInputPort.execute("user-1", "V-9"))
                                .thenReturn(sample("V-9", "CLAIMED"));

                mockMvc.perform(get("/api/v1/promotions/vouchers/me/V-9"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.voucherCode").value("V-9"));
        }

        @Test
        void summaryReturnsAvailableCounts() throws Exception {
                when(getVoucherSummaryInputPort.execute())
                                .thenReturn(new VoucherSummaryResult(10, 4, 5, 1));

                mockMvc.perform(get("/api/v1/promotions/vouchers/summary"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.all").value(10))
                                .andExpect(jsonPath("$.data.platform").value(4))
                                .andExpect(jsonPath("$.data.shop").value(5))
                                .andExpect(jsonPath("$.data.shipping").value(1));
        }

        @Test
        void adminGrantReturnsUserVoucher() throws Exception {
                when(grantVoucherInputPort.execute(any(VoucherCommands.GrantVoucher.class)))
                                .thenReturn(sample("LATEDELIVERY15K", "CLAIMED"));

                mockMvc.perform(post("/api/v1/promotions/vouchers/admin/grant")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "userId": "user-1",
                                                  "voucherCode": "LATEDELIVERY15K",
                                                  "reason": "Late delivery compensation"
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.voucherCode").value("LATEDELIVERY15K"))
                                .andExpect(jsonPath("$.data.status").value("CLAIMED"));

                verify(grantVoucherInputPort).execute(any(VoucherCommands.GrantVoucher.class));
        }
}
