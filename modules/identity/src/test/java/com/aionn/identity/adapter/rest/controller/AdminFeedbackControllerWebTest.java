package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.feedback.request.AdminChangeFeedbackStatusRequest;
import com.aionn.identity.adapter.rest.dto.feedback.request.AdminReplyFeedbackRequest;
import com.aionn.identity.adapter.rest.dto.feedback.response.FeedbackAnalyticsResponse;
import com.aionn.identity.adapter.rest.dto.feedback.response.FeedbackResponse;
import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.adapter.rest.mapper.feedback.FeedbackDtoMapper;
import com.aionn.identity.adapter.rest.support.MockAuthenticationArgumentResolver;
import com.aionn.identity.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.identity.application.dto.analytics.result.FeedbackAnalyticsResult;
import com.aionn.identity.application.dto.common.PageResult;
import com.aionn.identity.application.dto.feedback.command.AdminFeedbackCommands;
import com.aionn.identity.application.dto.feedback.result.FeedbackResult;
import com.aionn.identity.application.port.in.feedback.ChangeFeedbackStatusInputPort;
import com.aionn.identity.application.port.in.feedback.GetAdminFeedbackQueryPort;
import com.aionn.identity.application.port.in.feedback.GetFeedbackAnalyticsQueryPort;
import com.aionn.identity.application.port.in.feedback.ListAdminFeedbackQueryPort;
import com.aionn.identity.application.port.in.feedback.ReplyFeedbackInputPort;
import com.aionn.identity.domain.valueobject.FeedbackStatus;
import com.aionn.sharedkernel.adapter.web.response.PageMetadata;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminFeedbackControllerWebTest {

    @Mock private ListAdminFeedbackQueryPort listAdminFeedbackQueryPort;
    @Mock private GetAdminFeedbackQueryPort getAdminFeedbackQueryPort;
    @Mock private ReplyFeedbackInputPort replyFeedbackInputPort;
    @Mock private ChangeFeedbackStatusInputPort changeFeedbackStatusInputPort;
    @Mock private GetFeedbackAnalyticsQueryPort getFeedbackAnalyticsQueryPort;
    @Mock private FeedbackDtoMapper feedbackDtoMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminFeedbackController controller = new AdminFeedbackController(
                listAdminFeedbackQueryPort, getAdminFeedbackQueryPort,
                replyFeedbackInputPort, changeFeedbackStatusInputPort,
                getFeedbackAnalyticsQueryPort, feedbackDtoMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IdentityExceptionHandler())
                .addInterceptors(new MockSecurityInterceptor())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .setCustomArgumentResolvers(new MockAuthenticationArgumentResolver())
                .build();
    }

    private static FeedbackResult sampleResult(String feedbackId, String status,
            String handledBy, LocalDateTime handledAt, String adminReply) {
        LocalDateTime now = LocalDateTime.now();
        return new FeedbackResult(feedbackId, "u-1", null, null, "c",
                null, null, null, status, handledBy, handledAt, adminReply, now);
    }

    private static FeedbackResponse sampleResponse(String feedbackId, String status,
            String handledBy, LocalDateTime handledAt, String adminReply) {
        LocalDateTime now = LocalDateTime.now();
        return new FeedbackResponse(feedbackId, "u-1", null, null, "c",
                null, null, null, status, handledBy, handledAt, adminReply, now);
    }

    @Test
    void analyticsReturnsAnalyticsResponse() throws Exception {
        FeedbackAnalyticsResult result = new FeedbackAnalyticsResult(
                LocalDate.of(2024, Month.JANUARY, 1), LocalDate.of(2024, Month.JANUARY, 31),
                5, 10, 2, 3.5, List.of(new FeedbackAnalyticsResult.CategoryCount("BUG", 3L)));
        FeedbackAnalyticsResponse response = new FeedbackAnalyticsResponse(
                LocalDate.of(2024, Month.JANUARY, 1), LocalDate.of(2024, Month.JANUARY, 31),
                5, 10, 2, 3.5, List.of(new FeedbackAnalyticsResponse.CategoryCount("BUG", 3L)));

        when(getFeedbackAnalyticsQueryPort.execute(LocalDate.of(2024, Month.JANUARY, 1), LocalDate.of(2024, Month.JANUARY, 31)))
                .thenReturn(result);
        when(feedbackDtoMapper.toAnalyticsResponse(result)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/feedbacks/analytics")
                        .param("from", "2024-01-01").param("to", "2024-01-31")
                        .with(user("admin@example.com").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.open").value(5))
                .andExpect(jsonPath("$.data.byCategory[0].category").value("BUG"));
    }

    @Test
    void listReturnsPaginatedFeedbacks() throws Exception {
        FeedbackResult r1 = sampleResult("fb-1", "OPEN", null, null, null);
        FeedbackResponse resp1 = sampleResponse("fb-1", "OPEN", null, null, null);
        PageResult<FeedbackResult> page = new PageResult<>(List.of(r1), 0, 20, 1L);

        when(listAdminFeedbackQueryPort.execute(FeedbackStatus.OPEN, 0, 20)).thenReturn(page);
        when(feedbackDtoMapper.toResponses(page.content())).thenReturn(List.of(resp1));
        when(feedbackDtoMapper.toPageMetadata(page)).thenReturn(new PageMetadata(0, 20, 1L, 1));

        mockMvc.perform(get("/api/v1/admin/feedbacks")
                        .param("status", "OPEN")
                        .with(user("admin@example.com").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].feedbackId").value("fb-1"))
                .andExpect(jsonPath("$.paging.totalElements").value(1));
    }

    @Test
    void getReturnsSingleFeedback() throws Exception {
        FeedbackResult r = sampleResult("fb-9", "OPEN", null, null, null);
        FeedbackResponse resp = sampleResponse("fb-9", "OPEN", null, null, null);

        when(getAdminFeedbackQueryPort.execute("fb-9")).thenReturn(r);
        when(feedbackDtoMapper.toResponse(r)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/admin/feedbacks/fb-9")
                        .with(user("admin@example.com").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackId").value("fb-9"));
    }

    @Test
    void replyPersistsAdminReply() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        FeedbackResult r = sampleResult("fb-1", "IN_REVIEW", "admin@example.com", now, "thanks");
        FeedbackResponse resp = sampleResponse("fb-1", "IN_REVIEW", "admin@example.com", now, "thanks");

        when(feedbackDtoMapper.toReplyCommand(any(), any(), any(AdminReplyFeedbackRequest.class)))
                .thenReturn(new AdminFeedbackCommands.ReplyFeedback("fb-1", "admin@example.com", "thanks", null));
        when(replyFeedbackInputPort.execute(any())).thenReturn(r);
        when(feedbackDtoMapper.toResponse(r)).thenReturn(resp);

        mockMvc.perform(post("/api/v1/admin/feedbacks/fb-1/reply")
                        .with(user("admin@example.com").roles("SYSTEM_ADMIN"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "reply": "thanks" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminReply").value("thanks"));

        verify(replyFeedbackInputPort).execute(any());
    }

    @Test
    void changeStatusUpdatesStatus() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        FeedbackResult r = sampleResult("fb-1", "RESOLVED", "admin@example.com", now, null);
        FeedbackResponse resp = sampleResponse("fb-1", "RESOLVED", "admin@example.com", now, null);

        when(feedbackDtoMapper.toChangeStatusCommand(any(), any(), any(AdminChangeFeedbackStatusRequest.class)))
                .thenReturn(new AdminFeedbackCommands.ChangeFeedbackStatus("fb-1", "admin@example.com", FeedbackStatus.RESOLVED));
        when(changeFeedbackStatusInputPort.execute(any())).thenReturn(r);
        when(feedbackDtoMapper.toResponse(r)).thenReturn(resp);

        mockMvc.perform(put("/api/v1/admin/feedbacks/fb-1/status")
                        .with(user("admin@example.com").roles("SYSTEM_ADMIN"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "status": "RESOLVED" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        verify(changeFeedbackStatusInputPort).execute(any());
    }
}
