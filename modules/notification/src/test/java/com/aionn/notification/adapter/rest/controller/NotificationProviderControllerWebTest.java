package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.exception.NotificationExceptionHandler;
import com.aionn.notification.adapter.rest.support.session.CurrentAdminIdArgumentResolver;
import com.aionn.notification.application.dto.analytics.result.AnalyticsResult;
import com.aionn.notification.application.dto.provider.command.ProviderCommands;
import com.aionn.notification.application.dto.provider.result.ProviderResult;
import com.aionn.notification.application.service.NotificationAnalyticsService;
import com.aionn.notification.application.service.NotificationProviderService;
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

    @Mock
    private NotificationProviderService providerService;
    @Mock
    private NotificationAnalyticsService analyticsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        NotificationProviderController controller =
                new NotificationProviderController(providerService, analyticsService);

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

    @Test
    void configureCreatesProvider() throws Exception {
        Instant now = Instant.now();
        ProviderResult result = new ProviderResult(
                "prov-1", "EMAIL", "smtp", Map.of("host", "smtp.test"),
                true, 60, "admin-1", now, now);
        when(providerService.configure(any(ProviderCommands.ConfigureProvider.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/notifications/providers")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "channel": "EMAIL",
                                  "providerType": "smtp",
                                  "config": {"host": "smtp.test"},
                                  "rateLimitPerMinute": 60
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.providerId").value("prov-1"))
                .andExpect(jsonPath("$.data.channel").value("EMAIL"));

        verify(providerService).configure(any(ProviderCommands.ConfigureProvider.class));
    }

    @Test
    void updateUpdatesProvider() throws Exception {
        Instant now = Instant.now();
        ProviderResult result = new ProviderResult(
                "prov-1", "EMAIL", "smtp", Map.of("host", "smtp.new"),
                false, 120, "admin-1", now, now);
        when(providerService.update(any(ProviderCommands.UpdateProvider.class))).thenReturn(result);

        mockMvc.perform(put("/api/v1/notifications/providers/prov-1")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "config": {"host": "smtp.new"},
                                  "rateLimitPerMinute": 120,
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerId").value("prov-1"))
                .andExpect(jsonPath("$.data.active").value(false));

        verify(providerService).update(any(ProviderCommands.UpdateProvider.class));
    }

    @Test
    void listFetchesProviders() throws Exception {
        Instant now = Instant.now();
        ProviderResult r1 = new ProviderResult(
                "prov-1", "EMAIL", "smtp", Map.of(), true, 60, "admin-1", now, now);
        ProviderResult r2 = new ProviderResult(
                "prov-2", "SMS", "twilio", Map.of(), true, 30, "admin-1", now, now);
        when(providerService.listAll()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/v1/notifications/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].providerId").value("prov-1"))
                .andExpect(jsonPath("$.data[1].channel").value("SMS"));

        verify(providerService).listAll();
    }

    @Test
    void analyticsReturnsReport() throws Exception {
        Instant now = Instant.now();
        AnalyticsResult result = new AnalyticsResult("rep-1", "campaign-1", 10, 5, 1, now);
        when(analyticsService.report("campaign-1")).thenReturn(result);

        mockMvc.perform(get("/api/v1/notifications/analytics")
                        .param("campaignId", "campaign-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value("rep-1"))
                .andExpect(jsonPath("$.data.sentCount").value(10));

        verify(analyticsService).report("campaign-1");
    }

    @Test
    void configureRejectsBlankProviderType() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/providers")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "channel": "EMAIL",
                                  "providerType": "",
                                  "config": {},
                                  "rateLimitPerMinute": 60
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));
    }
}
