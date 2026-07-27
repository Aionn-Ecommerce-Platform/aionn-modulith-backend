package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.exception.NotificationExceptionHandler;
import com.aionn.notification.adapter.rest.mapper.provider.NotificationProviderDtoMapperImpl;
import com.aionn.notification.adapter.rest.support.session.CurrentAdminIdArgumentResolver;
import com.aionn.notification.application.dto.analytics.result.AnalyticsResult;
import com.aionn.notification.application.dto.provider.command.ProviderCommands;
import com.aionn.notification.application.dto.provider.result.ProviderResult;
import com.aionn.notification.application.port.in.analytics.GetCampaignAnalyticsInputPort;
import com.aionn.notification.application.port.in.provider.ConfigureProviderInputPort;
import com.aionn.notification.application.port.in.provider.ListProvidersInputPort;
import com.aionn.notification.application.port.in.provider.UpdateProviderInputPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationProviderControllerWebTest {

        private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

        @Mock
        private ConfigureProviderInputPort configureProviderInputPort;
        @Mock
        private UpdateProviderInputPort updateProviderInputPort;
        @Mock
        private ListProvidersInputPort listProvidersInputPort;
        @Mock
        private GetCampaignAnalyticsInputPort getCampaignAnalyticsInputPort;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                NotificationProviderController controller = new NotificationProviderController(
                                configureProviderInputPort, updateProviderInputPort, listProvidersInputPort,
                                getCampaignAnalyticsInputPort, new NotificationProviderDtoMapperImpl());

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new NotificationExceptionHandler())
                                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                                                Jackson2ObjectMapperBuilder.json().build()))
                                .setCustomArgumentResolvers(new CurrentAdminIdArgumentResolver())
                                .build();

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                                "admin-1", "n/a",
                                                List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        private static ProviderResult sample(String providerId) {
                return new ProviderResult(providerId, "EMAIL", "smtp", Map.of("host", "localhost"),
                                true, 60, "admin-1", NOW, NOW);
        }

        @Test
        void configureUsesCurrentAdminAsConfiguredBy() throws Exception {
                when(configureProviderInputPort.execute(any(ProviderCommands.ConfigureProvider.class)))
                                .thenReturn(sample("prov-1"));

                mockMvc.perform(post("/api/v1/notifications/providers")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "channel": "EMAIL",
                                                  "providerType": "smtp",
                                                  "config": {"host": "localhost"},
                                                  "rateLimitPerMinute": 60
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.providerId").value("prov-1"))
                                .andExpect(jsonPath("$.data.config.host").value("localhost"));

                ArgumentCaptor<ProviderCommands.ConfigureProvider> captor = ArgumentCaptor
                                .forClass(ProviderCommands.ConfigureProvider.class);
                verify(configureProviderInputPort).execute(captor.capture());
                assertThat(captor.getValue().configuredBy()).isEqualTo("admin-1");
        }

        @Test
        void configureRejectsBlankProviderType() throws Exception {
                mockMvc.perform(post("/api/v1/notifications/providers")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "channel": "EMAIL",
                                                  "providerType": "",
                                                  "rateLimitPerMinute": 60
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void configureRejectsNonPositiveRateLimit() throws Exception {
                mockMvc.perform(post("/api/v1/notifications/providers")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "channel": "EMAIL",
                                                  "providerType": "smtp",
                                                  "rateLimitPerMinute": 0
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void updatePassesProviderIdAndAdmin() throws Exception {
                when(updateProviderInputPort.execute(any(ProviderCommands.UpdateProvider.class)))
                                .thenReturn(sample("prov-9"));

                mockMvc.perform(put("/api/v1/notifications/providers/prov-9")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "rateLimitPerMinute": 120,
                                                  "active": false
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Provider updated"));

                ArgumentCaptor<ProviderCommands.UpdateProvider> captor = ArgumentCaptor
                                .forClass(ProviderCommands.UpdateProvider.class);
                verify(updateProviderInputPort).execute(captor.capture());
                assertThat(captor.getValue().providerId()).isEqualTo("prov-9");
                assertThat(captor.getValue().configuredBy()).isEqualTo("admin-1");
        }

        @Test
        void listReturnsProviders() throws Exception {
                when(listProvidersInputPort.execute()).thenReturn(List.of(sample("prov-1"), sample("prov-2")));

                mockMvc.perform(get("/api/v1/notifications/providers"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[1].providerId").value("prov-2"));
        }

        @Test
        void analyticsReturnsCampaignReport() throws Exception {
                when(getCampaignAnalyticsInputPort.execute("camp-1"))
                                .thenReturn(new AnalyticsResult("ana-1", "camp-1", 10, 4, 1, NOW));

                mockMvc.perform(get("/api/v1/notifications/analytics").param("campaignId", "camp-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.sentCount").value(10))
                                .andExpect(jsonPath("$.data.readCount").value(4))
                                .andExpect(jsonPath("$.message").value("Analytics generated"));
        }

        @Test
        void analyticsRequiresCampaignId() throws Exception {
                mockMvc.perform(get("/api/v1/notifications/analytics"))
                                .andExpect(status().isBadRequest());
        }
}
