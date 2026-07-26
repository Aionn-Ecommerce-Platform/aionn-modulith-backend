package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.exception.NotificationExceptionHandler;
import com.aionn.notification.adapter.rest.support.session.CurrentAdminIdArgumentResolver;
import com.aionn.notification.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.dto.notification.result.NotificationResult;
import com.aionn.notification.application.service.NotificationDispatchService;
import com.aionn.notification.domain.valueobject.NotificationCategory;
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
class NotificationControllerWebTest {

    @Mock
    private NotificationDispatchService dispatchService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        NotificationController controller = new NotificationController(dispatchService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new NotificationExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .setCustomArgumentResolvers(
                        new CurrentUserIdArgumentResolver(),
                        new CurrentAdminIdArgumentResolver())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user-123", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dispatchSendsNotification() throws Exception {
        Instant now = Instant.now();
        NotificationResult result = new NotificationResult(
                "noti-1", "user-123", "tpl-1", "EMAIL", "SECURITY", "CRITICAL",
                "Subject", "Content", "campaign-1", "SENT", 0, null,
                now, now, now, null, null);
        when(dispatchService.sendByEvent(any(NotificationCommands.SendByEvent.class)))
                .thenReturn(List.of(result));

        mockMvc.perform(post("/api/v1/notifications/dispatch")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-123",
                                  "eventType": "ORDER_PLACED",
                                  "category": "TRANSACTION",
                                  "channels": ["EMAIL"],
                                  "locale": "vi-VN",
                                  "campaignId": "campaign-1",
                                  "context": {"orderId": "ord-1"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].notiId").value("noti-1"))
                .andExpect(jsonPath("$.message").value("Notifications dispatched"));

        verify(dispatchService).sendByEvent(any(NotificationCommands.SendByEvent.class));
    }

    @Test
    void markReadMarksNotificationAsRead() throws Exception {
        Instant now = Instant.now();
        NotificationResult result = new NotificationResult(
                "noti-2", "user-123", "tpl-1", "EMAIL", "SECURITY", "CRITICAL",
                "Subject", "Content", null, "READ", 0, null,
                now, now, now, now, null);
        when(dispatchService.markRead(any(NotificationCommands.MarkRead.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/notifications/noti-2/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notiId").value("noti-2"))
                .andExpect(jsonPath("$.data.status").value("READ"));

        verify(dispatchService).markRead(any(NotificationCommands.MarkRead.class));
    }

    @Test
    void deleteSoftDeletesNotification() throws Exception {
        Instant now = Instant.now();
        NotificationResult result = new NotificationResult(
                "noti-3", "user-123", "tpl-1", "EMAIL", "SECURITY", "CRITICAL",
                "Subject", "Content", null, "DELETED", 0, null,
                now, now, now, null, now);
        when(dispatchService.delete(any(NotificationCommands.MarkDeleted.class))).thenReturn(result);

        mockMvc.perform(delete("/api/v1/notifications/noti-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notiId").value("noti-3"))
                .andExpect(jsonPath("$.data.status").value("DELETED"));

        verify(dispatchService).delete(any(NotificationCommands.MarkDeleted.class));
    }

    @Test
    void getFetchesNotification() throws Exception {
        Instant now = Instant.now();
        NotificationResult result = new NotificationResult(
                "noti-4", "user-123", "tpl-1", "EMAIL", "SECURITY", "CRITICAL",
                "Subject", "Content", null, "SENT", 0, null,
                now, now, now, null, null);
        when(dispatchService.get("user-123", "noti-4")).thenReturn(result);

        mockMvc.perform(get("/api/v1/notifications/noti-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notiId").value("noti-4"));

        verify(dispatchService).get("user-123", "noti-4");
    }

    @Test
    void listMineFetchesMyNotifications() throws Exception {
        Instant now = Instant.now();
        NotificationResult r1 = new NotificationResult(
                "noti-5", "user-123", "tpl-1", "EMAIL", "SECURITY", "CRITICAL",
                "Subject", "Content", null, "SENT", 0, null,
                now, now, now, null, null);
        when(dispatchService.listMine("user-123", 50)).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].notiId").value("noti-5"));

        verify(dispatchService).listMine("user-123", 50);
    }

    @Test
    void dispatchRejectsBlankUserId() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/dispatch")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "",
                                  "eventType": "ORDER_PLACED",
                                  "category": "TRANSACTION"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));
    }

    // Reference NotificationCategory so it is treated as used by static analysis.
    @SuppressWarnings("unused")
    private static final NotificationCategory CATEGORY = NotificationCategory.TRANSACTION;
}
