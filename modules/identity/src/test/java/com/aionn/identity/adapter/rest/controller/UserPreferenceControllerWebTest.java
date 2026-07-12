package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.preference.request.AiPrivacyPreferenceRequest;
import com.aionn.identity.adapter.rest.dto.preference.request.GeneralPreferenceRequest;
import com.aionn.identity.adapter.rest.dto.preference.request.NotificationPreferenceRequest;
import com.aionn.identity.adapter.rest.dto.preference.response.UserPreferenceResponse;
import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.adapter.rest.mapper.preference.UserPreferenceDtoMapper;
import com.aionn.identity.adapter.rest.support.MockAuthenticationArgumentResolver;
import com.aionn.identity.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.identity.application.dto.preference.command.UpdateAiPrivacyPreferenceCommand;
import com.aionn.identity.application.dto.preference.command.UpdateGeneralPreferenceCommand;
import com.aionn.identity.application.dto.preference.command.UpdateNotificationPreferenceCommand;
import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;
import com.aionn.identity.application.port.in.preference.GetUserPreferenceQueryPort;
import com.aionn.identity.application.port.in.preference.UpdateAiPrivacyPreferenceInputPort;
import com.aionn.identity.application.port.in.preference.UpdateGeneralPreferenceInputPort;
import com.aionn.identity.application.port.in.preference.UpdateNotificationPreferenceInputPort;
import com.aionn.sharedkernel.adapter.web.support.clientip.ClientIpArgumentResolver;
import com.aionn.sharedkernel.infrastructure.web.ClientIpResolver;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web tests for UserPreferenceController covering:
 * - read (general preferences),
 * - update general/notification/AI-privacy preferences,
 * - authentication enforcement.
 */
@ExtendWith(MockitoExtension.class)
class UserPreferenceControllerWebTest {

        @Mock
        private GetUserPreferenceQueryPort getUserPreferenceQueryPort;
        @Mock
        private UpdateGeneralPreferenceInputPort updateGeneralPreferenceInputPort;
        @Mock
        private UpdateNotificationPreferenceInputPort updateNotificationPreferenceInputPort;
        @Mock
        private UpdateAiPrivacyPreferenceInputPort updateAiPrivacyPreferenceInputPort;
        @Mock
        private UserPreferenceDtoMapper userPreferenceDtoMapper;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                UserPreferenceController controller = new UserPreferenceController(
                                getUserPreferenceQueryPort, updateGeneralPreferenceInputPort,
                                updateNotificationPreferenceInputPort, updateAiPrivacyPreferenceInputPort,
                                userPreferenceDtoMapper);

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new IdentityExceptionHandler())
                                .addInterceptors(new MockSecurityInterceptor())
                                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                                                Jackson2ObjectMapperBuilder.json().build()))
                                .setCustomArgumentResolvers(
                                                new ClientIpArgumentResolver(new ClientIpResolver()),
                                                new MockAuthenticationArgumentResolver())
                                .build();
        }

        private UserPreferenceResult sampleResult() {
                return new UserPreferenceResult(
                                "user-123",
                                "vi",
                                "VND",
                                "Asia/Ho_Chi_Minh",
                                "dark",
                                "{\"orders\":true,\"promotions\":false}",
                                "{\"allowAiAnalysis\":false}",
                                Instant.now());
        }

        private UserPreferenceResponse sampleResponse() {
                return new UserPreferenceResponse(
                                "user-123",
                                "vi",
                                "VND",
                                "Asia/Ho_Chi_Minh",
                                "dark",
                                "{\"orders\":true,\"promotions\":false}",
                                "{\"allowAiAnalysis\":false}",
                                Instant.now());
        }

        @Test
        void getPreferencesReturnsCurrentSettings() throws Exception {
                UserPreferenceResult result = sampleResult();
                UserPreferenceResponse response = sampleResponse();

                when(getUserPreferenceQueryPort.execute("alice@example.com")).thenReturn(result);
                when(userPreferenceDtoMapper.toResponse(result)).thenReturn(response);

                mockMvc.perform(get("/api/v1/preferences")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.userId").value("user-123"))
                                .andExpect(jsonPath("$.data.language").value("vi"))
                                .andExpect(jsonPath("$.data.currency").value("VND"))
                                .andExpect(jsonPath("$.data.theme").value("dark"));

                verify(getUserPreferenceQueryPort).execute("alice@example.com");
        }

        @Test
        void updateGeneralUpdatesAndReturnsPreferences() throws Exception {
                UserPreferenceResult result = sampleResult();
                UserPreferenceResponse response = sampleResponse();
                UpdateGeneralPreferenceCommand command = new UpdateGeneralPreferenceCommand(
                                "user-123", "vi", "VND", "Asia/Ho_Chi_Minh", "dark");

                when(userPreferenceDtoMapper.toUpdateGeneralCommand(eq("alice@example.com"),
                                any(GeneralPreferenceRequest.class))).thenReturn(command);
                when(updateGeneralPreferenceInputPort.execute(command)).thenReturn(result);
                when(userPreferenceDtoMapper.toResponse(result)).thenReturn(response);

                mockMvc.perform(put("/api/v1/preferences/general")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "language": "vi",
                                                  "currency": "VND",
                                                  "timezone": "Asia/Ho_Chi_Minh",
                                                  "theme": "dark"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.language").value("vi"))
                                .andExpect(jsonPath("$.data.theme").value("dark"))
                                .andExpect(jsonPath("$.message").value("General preferences updated"));

                verify(updateGeneralPreferenceInputPort).execute(command);
        }

        @Test
        void updateNotificationsUpdatesAndReturnsPreferences() throws Exception {
                UserPreferenceResult result = sampleResult();
                UserPreferenceResponse response = sampleResponse();
                UpdateNotificationPreferenceCommand command = new UpdateNotificationPreferenceCommand(
                                "user-123", "{\"orders\":true,\"promotions\":false}");

                when(userPreferenceDtoMapper.toUpdateNotificationsCommand(eq("alice@example.com"),
                                any(NotificationPreferenceRequest.class))).thenReturn(command);
                when(updateNotificationPreferenceInputPort.execute(command)).thenReturn(result);
                when(userPreferenceDtoMapper.toResponse(result)).thenReturn(response);

                mockMvc.perform(put("/api/v1/preferences/notifications")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "notificationSettingsJson": "{\\"orders\\":true,\\"promotions\\":false}"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Notification preferences updated"));

                verify(updateNotificationPreferenceInputPort).execute(command);
        }

        @Test
        void updateAiPrivacyUpdatesAndReturnsPreferences() throws Exception {
                UserPreferenceResult result = sampleResult();
                UserPreferenceResponse response = sampleResponse();
                UpdateAiPrivacyPreferenceCommand command = new UpdateAiPrivacyPreferenceCommand(
                                "user-123", "{\"allowAiAnalysis\":false}");

                when(userPreferenceDtoMapper.toUpdateAiPrivacyCommand(eq("alice@example.com"),
                                any(AiPrivacyPreferenceRequest.class))).thenReturn(command);
                when(updateAiPrivacyPreferenceInputPort.execute(command)).thenReturn(result);
                when(userPreferenceDtoMapper.toResponse(result)).thenReturn(response);

                mockMvc.perform(put("/api/v1/preferences/ai-privacy")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "aiPrivacySettingsJson": "{\\"allowAiAnalysis\\":false}"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("AI privacy preferences updated"));

                verify(updateAiPrivacyPreferenceInputPort).execute(command);
        }

        @Test
        void unauthenticatedRequestReturns401() throws Exception {
                mockMvc.perform(get("/api/v1/preferences"))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(getUserPreferenceQueryPort);
        }
}
