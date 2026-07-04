package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.kyc.response.KycAnalyticsResponse;
import com.aionn.identity.adapter.rest.dto.kyc.response.KycResponse;
import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.adapter.rest.mapper.kyc.KycDtoMapper;
import com.aionn.identity.adapter.rest.support.MockAuthenticationArgumentResolver;
import com.aionn.identity.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.identity.application.dto.analytics.result.KycAnalyticsResult;
import com.aionn.identity.application.dto.kyc.command.KycAdminCommands;
import com.aionn.identity.application.dto.kyc.result.KycResult;
import com.aionn.identity.application.port.in.kyc.ApproveKycInputPort;
import com.aionn.identity.application.port.in.kyc.GetAdminKycQueryPort;
import com.aionn.identity.application.port.in.kyc.GetKycAnalyticsQueryPort;
import com.aionn.identity.application.port.in.kyc.ListAdminKycQueryPort;
import com.aionn.identity.application.port.in.kyc.MarkKycInReviewInputPort;
import com.aionn.identity.application.port.in.kyc.RejectKycInputPort;
import com.aionn.identity.domain.valueobject.KycStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminKycControllerWebTest {

    @Mock private ListAdminKycQueryPort listAdminKycQueryPort;
    @Mock private GetAdminKycQueryPort getAdminKycQueryPort;
    @Mock private MarkKycInReviewInputPort markKycInReviewInputPort;
    @Mock private ApproveKycInputPort approveKycInputPort;
    @Mock private RejectKycInputPort rejectKycInputPort;
    @Mock private GetKycAnalyticsQueryPort getKycAnalyticsQueryPort;
    @Mock private KycDtoMapper kycDtoMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminKycController controller = new AdminKycController(
                listAdminKycQueryPort, getAdminKycQueryPort,
                markKycInReviewInputPort, approveKycInputPort, rejectKycInputPort,
                getKycAnalyticsQueryPort, kycDtoMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IdentityExceptionHandler())
                .addInterceptors(new MockSecurityInterceptor())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .setCustomArgumentResolvers(new MockAuthenticationArgumentResolver())
                .build();
    }

    private static KycResult sampleResult(String status) {
        LocalDateTime now = LocalDateTime.now();
        return new KycResult("kyc-1", "user-1", "ID_CARD", null, status,
                "sumsub", null, "basic-kyc-level", null, null, null, null, null, now, null);
    }

    private static KycResponse sampleResponse(String status) {
        LocalDateTime now = LocalDateTime.now();
        return new KycResponse("kyc-1", "user-1", "ID_CARD", null, status,
                "sumsub", null, "basic-kyc-level", null, null, null, null, null, now, null);
    }

    @Test
    void analyticsReturnsAnalyticsResponse() throws Exception {
        KycAnalyticsResult result = new KycAnalyticsResult(LocalDate.of(2024, Month.JANUARY, 1), LocalDate.of(2024, Month.JANUARY, 31),
                5, 3, 1, 10, 0.6, 24.0);
        KycAnalyticsResponse response = new KycAnalyticsResponse(LocalDate.of(2024, Month.JANUARY, 1), LocalDate.of(2024, Month.JANUARY, 31),
                5, 3, 1, 10, 0.6, 24.0);

        when(getKycAnalyticsQueryPort.execute(LocalDate.of(2024, Month.JANUARY, 1), LocalDate.of(2024, Month.JANUARY, 31)))
                .thenReturn(result);
        when(kycDtoMapper.toAnalyticsResponse(result)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/kyc/analytics")
                        .param("from", "2024-01-01").param("to", "2024-01-31")
                        .with(user("admin@example.com").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalRate").value(0.6));
    }

    @Test
    void listByStatusReturnsKycs() throws Exception {
        KycResult r = sampleResult("SUBMITTED");
        KycResponse resp = sampleResponse("SUBMITTED");

        when(listAdminKycQueryPort.execute(KycStatus.SUBMITTED, 50)).thenReturn(List.of(r));
        when(kycDtoMapper.toResponses(List.of(r))).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/v1/admin/kyc")
                        .with(user("admin@example.com").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].kycId").value("kyc-1"));
    }

    @Test
    void getReturnsKycById() throws Exception {
        KycResult r = sampleResult("SUBMITTED");
        KycResponse resp = sampleResponse("SUBMITTED");

        when(getAdminKycQueryPort.execute("kyc-1")).thenReturn(r);
        when(kycDtoMapper.toResponse(r)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/admin/kyc/kyc-1")
                        .with(user("admin@example.com").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kycId").value("kyc-1"));
    }

    @Test
    void markInReviewMovesToInReview() throws Exception {
        KycResult r = sampleResult("IN_REVIEW");
        KycResponse resp = sampleResponse("IN_REVIEW");

        when(markKycInReviewInputPort.execute(any(KycAdminCommands.MarkInReviewKyc.class))).thenReturn(r);
        when(kycDtoMapper.toResponse(r)).thenReturn(resp);

        mockMvc.perform(post("/api/v1/admin/kyc/kyc-1/in-review")
                        .with(user("admin@example.com").roles("SYSTEM_ADMIN"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "note": "starting review" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_REVIEW"));

        verify(markKycInReviewInputPort).execute(any());
    }

    @Test
    void markInReviewAcceptsEmptyBody() throws Exception {
        KycResult r = sampleResult("IN_REVIEW");
        KycResponse resp = sampleResponse("IN_REVIEW");

        when(markKycInReviewInputPort.execute(any(KycAdminCommands.MarkInReviewKyc.class))).thenReturn(r);
        when(kycDtoMapper.toResponse(r)).thenReturn(resp);

        mockMvc.perform(post("/api/v1/admin/kyc/kyc-1/in-review")
                        .with(user("admin@example.com").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void approveMovesToApproved() throws Exception {
        KycResult r = sampleResult("APPROVED");
        KycResponse resp = sampleResponse("APPROVED");

        when(approveKycInputPort.execute(any(KycAdminCommands.ApproveKyc.class))).thenReturn(r);
        when(kycDtoMapper.toResponse(r)).thenReturn(resp);

        mockMvc.perform(post("/api/v1/admin/kyc/kyc-1/approve")
                        .with(user("admin@example.com").roles("SYSTEM_ADMIN"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "note": "ok" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void rejectMovesToRejected() throws Exception {
        KycResult r = sampleResult("REJECTED");
        KycResponse resp = sampleResponse("REJECTED");

        when(rejectKycInputPort.execute(any(KycAdminCommands.RejectKyc.class))).thenReturn(r);
        when(kycDtoMapper.toResponse(r)).thenReturn(resp);

        mockMvc.perform(post("/api/v1/admin/kyc/kyc-1/reject")
                        .with(user("admin@example.com").roles("SYSTEM_ADMIN"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "reason": "blurry photo" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void rejectRequiresReason() throws Exception {
        mockMvc.perform(post("/api/v1/admin/kyc/kyc-1/reject")
                        .with(user("admin@example.com").roles("SYSTEM_ADMIN"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "reason": "" }
                                """))
                .andExpect(status().isBadRequest());
    }
}
