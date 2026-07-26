package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.exception.NotificationExceptionHandler;
import com.aionn.notification.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.notification.application.dto.subscription.command.SubscriptionCommands;
import com.aionn.notification.application.dto.subscription.result.DeviceTokenResult;
import com.aionn.notification.application.dto.subscription.result.SubscriptionResult;
import com.aionn.notification.application.service.NotificationSubscriptionService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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

    @Mock
    private NotificationSubscriptionService subscriptionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        NotificationSubscriptionController controller =
                new NotificationSubscriptionController(subscriptionService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new NotificationExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
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

    @Test
    void getMineFetchesSubscription() throws Exception {
        Instant now = Instant.now();
        SubscriptionResult result = new SubscriptionResult(
                "user-123",
                Map.of("TRANSACTION", Map.of("EMAIL", true)),
                now, now);
        when(subscriptionService.get("user-123")).thenReturn(result);

        mockMvc.perform(get("/api/v1/notifications/subscriptions/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("user-123"));

        verify(subscriptionService).get("user-123");
    }

    @Test
    void updateChannelUpdatesSubscription() throws Exception {
        Instant now = Instant.now();
        SubscriptionResult result = new SubscriptionResult(
                "user-123",
                Map.of("PROMOTION", Map.of("EMAIL", false)),
                now, now);
        when(subscriptionService.updateChannel(any(SubscriptionCommands.UpdateChannel.class)))
                .thenReturn(result);

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
                .andExpect(jsonPath("$.data.userId").value("user-123"));

        verify(subscriptionService).updateChannel(any(SubscriptionCommands.UpdateChannel.class));
    }

    @Test
    void registerDeviceCreatesToken() throws Exception {
        Instant now = Instant.now();
        DeviceTokenResult result = new DeviceTokenResult(
                "tok-1", "user-123", "device-token-abc", "iOS", true, now);
        when(subscriptionService.registerDeviceToken(any(SubscriptionCommands.RegisterDeviceToken.class)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/notifications/subscriptions/me/device-tokens")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceToken": "device-token-abc",
                                  "os": "iOS"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tokenId").value("tok-1"))
                .andExpect(jsonPath("$.data.deviceToken").value("device-token-abc"));

        verify(subscriptionService).registerDeviceToken(any(SubscriptionCommands.RegisterDeviceToken.class));
    }

    @Test
    void removeDeviceReturnsNoContent() throws Exception {
        doNothing().when(subscriptionService)
                .removeDeviceToken(any(SubscriptionCommands.RemoveDeviceToken.class));

        mockMvc.perform(delete("/api/v1/notifications/subscriptions/me/device-tokens/tok-1"))
                .andExpect(status().isNoContent());

        verify(subscriptionService).removeDeviceToken(any(SubscriptionCommands.RemoveDeviceToken.class));
    }

    @Test
    void listDevicesReturnsTokens() throws Exception {
        Instant now = Instant.now();
        DeviceTokenResult t1 = new DeviceTokenResult(
                "tok-1", "user-123", "device-token-abc", "iOS", true, now);
        DeviceTokenResult t2 = new DeviceTokenResult(
                "tok-2", "user-123", "device-token-def", "Android", true, now);
        when(subscriptionService.listDeviceTokens("user-123")).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/api/v1/notifications/subscriptions/me/device-tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tokenId").value("tok-1"))
                .andExpect(jsonPath("$.data[1].os").value("Android"));

        verify(subscriptionService).listDeviceTokens("user-123");
    }

    @Test
    void registerDeviceRejectsBlankToken() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/subscriptions/me/device-tokens")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceToken": "",
                                  "os": "iOS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));
    }
}
