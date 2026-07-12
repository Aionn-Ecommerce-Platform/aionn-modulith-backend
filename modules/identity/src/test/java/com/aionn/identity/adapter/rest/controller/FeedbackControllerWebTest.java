package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.feedback.request.SubmitFeedbackRequest;
import com.aionn.identity.adapter.rest.dto.feedback.response.FeedbackResponse;
import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.adapter.rest.mapper.feedback.FeedbackDtoMapper;
import com.aionn.identity.adapter.rest.support.MockAuthenticationArgumentResolver;
import com.aionn.identity.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.identity.application.dto.feedback.command.SubmitFeedbackCommand;
import com.aionn.identity.application.dto.feedback.result.FeedbackResult;
import com.aionn.identity.application.port.in.feedback.ListMyFeedbackQueryPort;
import com.aionn.identity.application.port.in.feedback.SubmitFeedbackInputPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
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
class FeedbackControllerWebTest {

    @Mock
    private SubmitFeedbackInputPort submitFeedbackInputPort;
    @Mock
    private ListMyFeedbackQueryPort listMyFeedbackQueryPort;
    @Mock
    private FeedbackDtoMapper feedbackDtoMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FeedbackController controller = new FeedbackController(
                submitFeedbackInputPort, listMyFeedbackQueryPort, feedbackDtoMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IdentityExceptionHandler())
                .addInterceptors(new MockSecurityInterceptor())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .setCustomArgumentResolvers(new MockAuthenticationArgumentResolver())
                .build();
    }

    @Test
    void submitAcceptsAuthenticatedFeedback() throws Exception {
        Instant now = Instant.now();
        FeedbackResult result = new FeedbackResult("fb-1", "user-1", "BUG", "sub", "content",
                5, "a@b.com", null, "OPEN", null, null, null, now);
        FeedbackResponse response = new FeedbackResponse("fb-1", "user-1", "BUG", "sub", "content",
                5, "a@b.com", null, "OPEN", null, null, null, now);

        when(feedbackDtoMapper.toSubmitCommand(any(), any(SubmitFeedbackRequest.class)))
                .thenReturn(new SubmitFeedbackCommand("user-1", "BUG", "sub", "content", 5, "a@b.com", null));
        when(submitFeedbackInputPort.execute(any())).thenReturn(result);
        when(feedbackDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(post("/api/v1/feedbacks")
                        .with(user("alice@example.com").roles("USER"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "category": "BUG", "subject": "sub", "content": "content", "rating": 5, "contactEmail": "a@b.com" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackId").value("fb-1"))
                .andExpect(jsonPath("$.message").value("Feedback submitted"));

        verify(submitFeedbackInputPort).execute(any());
    }

    @Test
    void listMineReturnsCurrentUserFeedbacks() throws Exception {
        Instant now = Instant.now();
        FeedbackResult r1 = new FeedbackResult("fb-a", "alice@example.com", "BUG", "s", "c",
                4, null, null, "OPEN", null, null, null, now);
        FeedbackResponse resp1 = new FeedbackResponse("fb-a", "alice@example.com", "BUG", "s", "c",
                4, null, null, "OPEN", null, null, null, now);

        when(listMyFeedbackQueryPort.execute("alice@example.com")).thenReturn(List.of(r1));
        when(feedbackDtoMapper.toResponses(List.of(r1))).thenReturn(List.of(resp1));

        mockMvc.perform(get("/api/v1/feedbacks/me")
                        .with(user("alice@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].feedbackId").value("fb-a"));

        verify(listMyFeedbackQueryPort).execute("alice@example.com");
    }
}
