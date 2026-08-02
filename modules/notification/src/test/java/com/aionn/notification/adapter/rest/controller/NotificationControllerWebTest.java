package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.exception.NotificationExceptionHandler;
import com.aionn.notification.adapter.rest.mapper.notification.NotificationDtoMapper;
import com.aionn.notification.adapter.rest.support.session.CurrentAdminIdArgumentResolver;
import com.aionn.notification.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.dto.notification.result.NotificationResult;
import com.aionn.notification.application.port.in.notification.DeleteNotificationInputPort;
import com.aionn.notification.application.port.in.notification.GetMyNotificationInputPort;
import com.aionn.notification.application.port.in.notification.ListMyNotificationsInputPort;
import com.aionn.notification.application.port.in.notification.MarkNotificationReadInputPort;
import com.aionn.notification.application.port.in.notification.SendNotificationByEventInputPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import com.aionn.sharedkernel.infrastructure.config.JacksonMapperFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerWebTest {

        private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

        @Mock
        private SendNotificationByEventInputPort sendNotificationByEventInputPort;
        @Mock
        private MarkNotificationReadInputPort markNotificationReadInputPort;
        @Mock
        private DeleteNotificationInputPort deleteNotificationInputPort;
        @Mock
        private GetMyNotificationInputPort getMyNotificationInputPort;
        @Mock
        private ListMyNotificationsInputPort listMyNotificationsInputPort;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                NotificationController controller = new NotificationController(
                                sendNotificationByEventInputPort, markNotificationReadInputPort,
                                deleteNotificationInputPort, getMyNotificationInputPort,
                                listMyNotificationsInputPort, Mappers.getMapper(NotificationDtoMapper.class));

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new NotificationExceptionHandler())
                                .setMessageConverters(new JacksonJsonHttpMessageConverter(
                                                JacksonMapperFactory.create()))
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

        private static NotificationResult sample(String notiId, String status) {
                return new NotificationResult(notiId, "user-123", "tpl-1", "EMAIL", "SECURITY", "CRITICAL",
                                "Subject", "Content", "campaign-1", status, 0, null,
                                NOW, NOW, NOW, null, null);
        }

        @Test
        void dispatchSendsNotification() throws Exception {
                when(sendNotificationByEventInputPort.execute(any(NotificationCommands.SendByEvent.class)))
                                .thenReturn(List.of(sample("noti-1", "SENT")));

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

                verify(sendNotificationByEventInputPort).execute(any(NotificationCommands.SendByEvent.class));
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

        @Test
        void dispatchRejectsMissingCategory() throws Exception {
                mockMvc.perform(post("/api/v1/notifications/dispatch")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "userId": "user-123",
                                                  "eventType": "ORDER_PLACED"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void markReadMarksNotificationAsRead() throws Exception {
                when(markNotificationReadInputPort.execute(any(NotificationCommands.MarkRead.class)))
                                .thenReturn(sample("noti-2", "READ"));

                mockMvc.perform(post("/api/v1/notifications/noti-2/read"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.notiId").value("noti-2"))
                                .andExpect(jsonPath("$.data.status").value("READ"));

                verify(markNotificationReadInputPort).execute(any(NotificationCommands.MarkRead.class));
        }

        @Test
        void deleteSoftDeletesNotification() throws Exception {
                when(deleteNotificationInputPort.execute(any(NotificationCommands.MarkDeleted.class)))
                                .thenReturn(sample("noti-3", "DELETED"));

                mockMvc.perform(delete("/api/v1/notifications/noti-3"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("DELETED"));

                verify(deleteNotificationInputPort).execute(any(NotificationCommands.MarkDeleted.class));
        }

        @Test
        void getFetchesNotification() throws Exception {
                when(getMyNotificationInputPort.execute("user-123", "noti-4"))
                                .thenReturn(sample("noti-4", "SENT"));

                mockMvc.perform(get("/api/v1/notifications/noti-4"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.notiId").value("noti-4"));
        }

        @Test
        void listMineUsesDefaultLimit() throws Exception {
                when(listMyNotificationsInputPort.execute("user-123", 50))
                                .thenReturn(List.of(sample("noti-5", "SENT")));

                mockMvc.perform(get("/api/v1/notifications"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].notiId").value("noti-5"));

                verify(listMyNotificationsInputPort).execute("user-123", 50);
        }

        @Test
        void listMineRejectsLimitAboveMaximum() throws Exception {
                mockMvc.perform(get("/api/v1/notifications").param("limit", "9999"))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(listMyNotificationsInputPort);
        }

        @Test
        void listMineRejectsLimitBelowMinimum() throws Exception {
                mockMvc.perform(get("/api/v1/notifications").param("limit", "0"))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(listMyNotificationsInputPort);
        }
}
