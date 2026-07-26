package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.exception.PromotionExceptionHandler;
import com.aionn.promotion.adapter.rest.mapper.voucher.VoucherDtoMapperImpl;
import com.aionn.promotion.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.dto.voucher.result.UserVoucherResult;
import com.aionn.promotion.application.port.in.voucher.ApplyVoucherInputPort;
import com.aionn.promotion.application.port.in.voucher.ClaimVoucherInputPort;
import com.aionn.promotion.application.port.in.voucher.GetMyVoucherInputPort;
import com.aionn.promotion.application.port.in.voucher.ListMyVouchersInputPort;
import com.aionn.promotion.application.port.in.voucher.ReleaseVoucherInputPort;
import com.aionn.promotion.application.port.in.voucher.ReserveVoucherInputPort;
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

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                VoucherController controller = new VoucherController(
                                claimVoucherInputPort, reserveVoucherInputPort, applyVoucherInputPort,
                                releaseVoucherInputPort, listMyVouchersInputPort, getMyVoucherInputPort,
                                new VoucherDtoMapperImpl());

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new PromotionExceptionHandler())
                                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                                                Jackson2ObjectMapperBuilder.json().build()))
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
                                new BigDecimal("50000"), "VND", "PLATFORM",
                                now.plusSeconds(86400), null, 100, 0);
        }

        @Test
        void claimReturnsUserVoucher() throws Exception {
                when(claimVoucherInputPort.execute(any(VoucherCommands.ClaimVoucher.class)))
                                .thenReturn(sample("V-1", "CLAIMED"));

                mockMvc.perform(post("/api/v1/promotions/vouchers/V-1/claim"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.voucherCode").value("V-1"))
                                .andExpect(jsonPath("$.data.status").value("CLAIMED"));

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
                when(listMyVouchersInputPort.execute("user-1", 50))
                                .thenReturn(List.of(sample("V-1", "CLAIMED"), sample("V-2", "RESERVED")));

                mockMvc.perform(get("/api/v1/promotions/vouchers/me"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].voucherCode").value("V-1"))
                                .andExpect(jsonPath("$.data[1].voucherCode").value("V-2"));
        }

        @Test
        void listMineCapsLimit() throws Exception {
                when(listMyVouchersInputPort.execute("user-1", 100)).thenReturn(List.of());

                mockMvc.perform(get("/api/v1/promotions/vouchers/me").param("limit", "999"))
                                .andExpect(status().isOk());

                verify(listMyVouchersInputPort).execute("user-1", 100);
        }

        @Test
        void getMineReturnsVoucher() throws Exception {
                when(getMyVoucherInputPort.execute("user-1", "V-9"))
                                .thenReturn(sample("V-9", "CLAIMED"));

                mockMvc.perform(get("/api/v1/promotions/vouchers/me/V-9"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.voucherCode").value("V-9"));
        }
}
