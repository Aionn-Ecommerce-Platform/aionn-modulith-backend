package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.consent.request.MarketingConsentRequest;
import com.aionn.identity.adapter.rest.dto.consent.request.TermsConsentRequest;
import com.aionn.identity.adapter.rest.dto.consent.response.ConsentResponse;
import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.adapter.rest.mapper.consent.ConsentDtoMapper;
import com.aionn.identity.adapter.rest.support.MockAuthenticationArgumentResolver;
import com.aionn.identity.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.identity.adapter.rest.support.client.AuthClientTypeArgumentResolver;
import com.aionn.identity.adapter.rest.support.client.ClientUserAgentArgumentResolver;
import com.aionn.identity.application.dto.consent.command.*;
import com.aionn.identity.application.dto.consent.result.ConsentResult;
import com.aionn.identity.application.port.in.consent.*;
import com.aionn.identity.infrastructure.config.properties.AuthProperties;
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
import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ConsentControllerWebTest {

        @Mock
        private GetMyConsentsQueryPort getMyConsentsQueryPort;
        @Mock
        private AgreeTermsInputPort agreeTermsInputPort;
        @Mock
        private AgreePrivacyInputPort agreePrivacyInputPort;
        @Mock
        private UpdateMarketingConsentInputPort updateMarketingConsentInputPort;
        @Mock
        private ConsentDtoMapper consentDtoMapper;
        @Mock
        private AuthProperties authProperties;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                ConsentController controller = new ConsentController(getMyConsentsQueryPort, agreeTermsInputPort,
                                agreePrivacyInputPort, updateMarketingConsentInputPort, consentDtoMapper);

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new IdentityExceptionHandler())
                                .addInterceptors(new MockSecurityInterceptor())
                                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                                                Jackson2ObjectMapperBuilder.json().build()))
                                .setCustomArgumentResolvers(
                                                new ClientIpArgumentResolver(new ClientIpResolver()),
                                                new ClientUserAgentArgumentResolver(),
                                                new AuthClientTypeArgumentResolver(authProperties),
                                                new MockAuthenticationArgumentResolver())
                                .build();
        }

        @Test
        void getMyConsentsReturnsAllConsentRecords() throws Exception {
                Instant now = Instant.now();
                ConsentResult consent1 = new ConsentResult("consent-1", "user-123", "TERMS", "v1.0", true, now, null,
                                "192.168.1.1");
                ConsentResult consent2 = new ConsentResult("consent-2", "user-123", "PRIVACY", "v2.0", true,
                                now.minus(Duration.ofDays(5)), null, "192.168.1.2");
                ConsentResult consent3 = new ConsentResult("consent-3", "user-123", "MARKETING", null, false, null,
                                null, null);
                List<ConsentResult> results = List.of(consent1, consent2, consent3);

                ConsentResponse resp1 = new ConsentResponse("consent-1", "TERMS", "v1.0", now, null, "192.168.1.1");
                ConsentResponse resp2 = new ConsentResponse("consent-2", "PRIVACY", "v2.0", now.minus(Duration.ofDays(5)), null,
                                "192.168.1.2");
                ConsentResponse resp3 = new ConsentResponse("consent-3", "MARKETING", null, null, null, null);

                when(getMyConsentsQueryPort.execute("alice@example.com")).thenReturn(results);
                when(consentDtoMapper.toResponses(results)).thenReturn(List.of(resp1, resp2, resp3));

                mockMvc.perform(get("/api/v1/consents")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].consentType").value("TERMS"))
                                .andExpect(jsonPath("$.data[1].consentType").value("PRIVACY"))
                                .andExpect(jsonPath("$.data[2].consentType").value("MARKETING"));

                verify(getMyConsentsQueryPort).execute("alice@example.com");
        }

        @Test
        void agreeTermsRecordsTermsConsent() throws Exception {
                Instant now = Instant.now();
                ConsentResult result = new ConsentResult("consent-terms-123", "user-123", "TERMS", "v1.5", true, now,
                                null, "192.168.1.1");
                ConsentResponse response = new ConsentResponse("consent-terms-123", "TERMS", "v1.5", now, null,
                                "192.168.1.1");

                when(consentDtoMapper.toTermsConsentCommand(eq("alice@example.com"), anyString(),
                                any(TermsConsentRequest.class)))
                                .thenReturn(new AgreeTermsCommand("user-123", "v1.5", "192.168.1.1"));
                when(agreeTermsInputPort.execute(any())).thenReturn(result);
                when(consentDtoMapper.toResponse(result)).thenReturn(response);

                mockMvc.perform(post("/api/v1/consents/terms")
                                .with(user("alice@example.com").roles("USER"))
                                .header("X-Forwarded-For", "192.168.1.1")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "version": "v1.5"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.consentType").value("TERMS"));

                verify(agreeTermsInputPort).execute(any());
        }

        @Test
        void agreePrivacyRecordsPrivacyConsent() throws Exception {
                Instant now = Instant.now();
                ConsentResult result = new ConsentResult("consent-privacy-456", "user-123", "PRIVACY", "v2.0", true,
                                now, null, "10.0.0.5");
                ConsentResponse response = new ConsentResponse("consent-privacy-456", "PRIVACY", "v2.0", now, null,
                                "10.0.0.5");

                when(consentDtoMapper.toPrivacyConsentCommand(eq("alice@example.com"), anyString(),
                                any(TermsConsentRequest.class)))
                                .thenReturn(new AgreePrivacyCommand("user-123", "v2.0", "10.0.0.5"));
                when(agreePrivacyInputPort.execute(any())).thenReturn(result);
                when(consentDtoMapper.toResponse(result)).thenReturn(response);

                mockMvc.perform(post("/api/v1/consents/privacy")
                                .with(user("alice@example.com").roles("USER"))
                                .header("X-Forwarded-For", "10.0.0.5")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "version": "v2.0"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.consentType").value("PRIVACY"));

                verify(agreePrivacyInputPort).execute(any());
        }

        @Test
        void updateMarketingUpdatesMarketingConsent() throws Exception {
                Instant now = Instant.now();
                ConsentResult result = new ConsentResult("consent-marketing-789", "user-123", "MARKETING", null, true,
                                now, null, "192.168.1.100");
                ConsentResponse response = new ConsentResponse("consent-marketing-789", "MARKETING", null, now, null,
                                "192.168.1.100");

                when(consentDtoMapper.toMarketingConsentCommand(eq("alice@example.com"), anyString(),
                                any(MarketingConsentRequest.class)))
                                .thenReturn(new UpdateMarketingConsentCommand("user-123", true, "192.168.1.100"));
                when(updateMarketingConsentInputPort.execute(any())).thenReturn(result);
                when(consentDtoMapper.toResponse(result)).thenReturn(response);

                mockMvc.perform(patch("/api/v1/consents/marketing")
                                .with(user("alice@example.com").roles("USER"))
                                .header("X-Forwarded-For", "192.168.1.100")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "subscribed": true
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.consentType").value("MARKETING"));

                verify(updateMarketingConsentInputPort).execute(any());
        }

        @Test
        void unauthorizedRequestReturns401() throws Exception {
                mockMvc.perform(get("/api/v1/consents"))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(getMyConsentsQueryPort);
        }
}
