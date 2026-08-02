package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.exception.NotificationExceptionHandler;
import com.aionn.notification.adapter.rest.mapper.subscription.NotificationSubscriptionDtoMapper;
import com.aionn.notification.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.notification.application.dto.subscription.command.SubscriptionCommands;
import com.aionn.notification.application.dto.subscription.result.DeviceTokenResult;
import com.aionn.notification.application.dto.subscription.result.SubscriptionResult;
import com.aionn.notification.application.port.in.subscription.GetMySubscriptionInputPort;
import com.aionn.notification.application.port.in.subscription.ListMyDeviceTokensInputPort;
import com.aionn.notification.application.port.in.subscription.RegisterDeviceTokenInputPort;
import com.aionn.notification.application.port.in.subscription.RemoveDeviceTokenInputPort;
import com.aionn.notification.application.port.in.subscription.UpdateSubscriptionChannelInputPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationSubscriptionControllerWebTest {

        private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

        @Mock
        private GetMySubscriptionInputPort getMySubscriptionInputPort;
        @Mock
        private UpdateSubscriptionChannelInputPort updateSubscriptionChannelInputPort;
        @Mock
        private RegisterDeviceTokenInputPort registerDeviceTokenInputPort;
        @Mock
        private RemoveDeviceTokenInputPort removeDeviceTokenInputPort;
        @Mock
        private ListMyDeviceTokensInputPort listMyDeviceTokensInputPort;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                NotificationSubscriptionController controller = new NotificationSubscriptionController(
                                getMySubscriptionInputPort, updateSubscriptionChannelInputPort,
                                registerDeviceTokenInputPort, removeDeviceTokenInputPort,
                                listMyDeviceTokensInputPort,
                                Mappers.getMapper(NotificationSubscriptionDtoMapper.class));

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new NotificationExceptionHandler())
                                .setMessageConverters(new JacksonJsonHttpMessageConverter(
                                                JacksonMapperFactory.create()))
                                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
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

        private static SubscriptionResult subscription() {
                return new SubscriptionResult("user-123",
                                Map.of("SECURITY", Map.of("EMAIL", true, "SMS", false)), NOW, NOW);
        }

        private static DeviceTokenResult deviceToken(String tokenId) {
                return new DeviceTokenResult(tokenId, "user-123", "fcm-token", "android", true, NOW);
        }

        @Test
        void getMineReturnsSubscription() throws Exception {
                when(getMySubscriptionInputPort.execute("user-123")).thenReturn(subscription());

                mockMvc.perform(get("/api/v1/notifications/subscriptions/me"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.userId").value("user-123"))
                                .andExpect(jsonPath("$.data.settings.SECURITY.EMAIL").value(true));
        }

        @Test
        void updateChannelPassesCurrentUser() throws Exception {
                when(updateSubscriptionChannelInputPort.execute(any(SubscriptionCommands.UpdateChannel.class)))
                                .thenReturn(subscription());

                mockMvc.perform(put("/api/v1/notifications/subscriptions/me")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "category": "PROMOTION",
                                                  "channel": "EMAIL",
                                                  "enabled": false
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Subscription updated"));

                ArgumentCaptor<SubscriptionCommands.UpdateChannel> captor = ArgumentCaptor
                                .forClass(SubscriptionCommands.UpdateChannel.class);
                verify(updateSubscriptionChannelInputPort).execute(captor.capture());
                assertThat(captor.getValue().userId()).isEqualTo("user-123");
                assertThat(captor.getValue().enabled()).isFalse();
        }

        @Test
        void updateChannelRejectsMissingCategory() throws Exception {
                mockMvc.perform(put("/api/v1/notifications/subscriptions/me")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "channel": "EMAIL",
                                                  "enabled": false
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void registerDeviceReturnsCreated() throws Exception {
                when(registerDeviceTokenInputPort.execute(any(SubscriptionCommands.RegisterDeviceToken.class)))
                                .thenReturn(deviceToken("tok-1"));

                mockMvc.perform(post("/api/v1/notifications/subscriptions/me/device-tokens")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "deviceToken": "fcm-token",
                                                  "os": "android"
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.tokenId").value("tok-1"))
                                .andExpect(jsonPath("$.data.active").value(true));
        }

        @Test
        void registerDeviceRejectsBlankToken() throws Exception {
                mockMvc.perform(post("/api/v1/notifications/subscriptions/me/device-tokens")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "deviceToken": "",
                                                  "os": "android"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void removeDeviceReturnsNoContent() throws Exception {
                mockMvc.perform(delete("/api/v1/notifications/subscriptions/me/device-tokens/tok-1"))
                                .andExpect(status().isNoContent());

                verify(removeDeviceTokenInputPort).execute(
                                new SubscriptionCommands.RemoveDeviceToken("user-123", "tok-1"));
        }

        @Test
        void listDevicesReturnsTokens() throws Exception {
                when(listMyDeviceTokensInputPort.execute("user-123"))
                                .thenReturn(List.of(deviceToken("tok-1"), deviceToken("tok-2")));

                mockMvc.perform(get("/api/v1/notifications/subscriptions/me/device-tokens"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[1].tokenId").value("tok-2"));
        }
}
