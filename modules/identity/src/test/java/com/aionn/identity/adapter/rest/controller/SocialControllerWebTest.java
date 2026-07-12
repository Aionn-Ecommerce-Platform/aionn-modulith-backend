package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.auth.request.LinkSocialRequest;
import com.aionn.identity.adapter.rest.dto.auth.response.SocialLinkResponse;
import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.adapter.rest.mapper.auth.AuthDtoMapper;
import com.aionn.identity.adapter.rest.support.MockAuthenticationArgumentResolver;
import com.aionn.identity.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.identity.application.dto.auth.command.LinkSocialCommand;
import com.aionn.identity.application.dto.auth.command.UnlinkSocialCommand;
import com.aionn.identity.application.dto.auth.result.SocialLinkResult;
import com.aionn.identity.application.port.in.auth.LinkSocialInputPort;
import com.aionn.identity.application.port.in.auth.UnlinkSocialInputPort;
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

@ExtendWith(MockitoExtension.class)
class SocialControllerWebTest {

        @Mock
        private LinkSocialInputPort linkSocialInputPort;
        @Mock
        private UnlinkSocialInputPort unlinkSocialInputPort;
        @Mock
        private AuthDtoMapper authDtoMapper;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                SocialController controller = new SocialController(linkSocialInputPort, unlinkSocialInputPort,
                                authDtoMapper);

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

        @Test
        void linkSocialSuccessfullyLinksAccountToUser() throws Exception {
                Instant now = Instant.now();
                SocialLinkResult result = new SocialLinkResult("GOOGLE", "google-sub-id-456", now);
                SocialLinkResponse response = new SocialLinkResponse("GOOGLE", "google-sub-id-456", now);

                when(authDtoMapper.toLinkSocialCommand(eq("alice@example.com"), any(LinkSocialRequest.class)))
                                .thenReturn(new LinkSocialCommand("user-123", "GOOGLE", "google-token-xyz"));
                when(linkSocialInputPort.execute(any())).thenReturn(result);
                when(authDtoMapper.toSocialLinkResponse(result)).thenReturn(response);

                mockMvc.perform(post("/api/v1/auth/social-links")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                 {
                                                   "provider": "GOOGLE",
                                                   "providerToken": "google-token-xyz"
                                                 }
                                                 """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.message").value("Social account linked successfully!"))
                                .andExpect(jsonPath("$.data.provider").value("GOOGLE"))
                                .andExpect(jsonPath("$.data.providerUserId").value("google-sub-id-456"));

                verify(linkSocialInputPort).execute(any());
        }

        @Test
        void unlinkSocialSuccessfullyUnlinksAccount() throws Exception {
                when(authDtoMapper.toUnlinkSocialCommand("alice@example.com", "GOOGLE"))
                                .thenReturn(new UnlinkSocialCommand("user-123", "GOOGLE"));
                doNothing().when(unlinkSocialInputPort).execute(any());

                mockMvc.perform(delete("/api/v1/auth/social-links/GOOGLE")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Social account unlinked"));

                verify(unlinkSocialInputPort).execute(any());
        }

        @Test
        void linkSocialWithoutAuthenticationReturns401() throws Exception {
                mockMvc.perform(post("/api/v1/auth/social-links")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                 {
                                                   "provider": "GOOGLE",
                                                   "providerToken": "token"
                                                 }
                                                 """))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(linkSocialInputPort);
        }

        @Test
        void unlinkSocialWithoutAuthenticationReturns401() throws Exception {
                mockMvc.perform(delete("/api/v1/auth/social-links/GOOGLE"))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(unlinkSocialInputPort);
        }
}
