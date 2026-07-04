package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.auth.response.AuthTokenResponse;
import com.aionn.identity.adapter.rest.dto.registration.request.CompleteRegistrationRequest;
import com.aionn.identity.adapter.rest.dto.registration.request.InitiateRegistrationRequest;
import com.aionn.identity.adapter.rest.dto.registration.response.RegistrationSessionResponse;
import com.aionn.identity.adapter.rest.mapper.registration.RegistrationDtoMapper;
import com.aionn.identity.adapter.rest.support.client.AuthClientTypeArgumentResolver;
import com.aionn.identity.adapter.rest.support.client.ClientUserAgentArgumentResolver;
import com.aionn.identity.adapter.rest.support.response.AuthTokenResponseHandler;
import com.aionn.identity.application.dto.registration.command.CompleteRegistrationCommand;
import com.aionn.identity.application.dto.registration.command.InitiateRegistrationCommand;
import com.aionn.identity.application.dto.registration.result.CompleteRegistrationResult;
import com.aionn.identity.application.dto.registration.result.InitiateRegistrationResult;
import com.aionn.identity.application.port.in.registration.CompleteRegistrationInputPort;
import com.aionn.identity.application.port.in.registration.InitiateRegistrationInputPort;
import com.aionn.identity.application.port.in.registration.ResendRegistrationOtpInputPort;
import com.aionn.identity.application.port.in.registration.VerifyRegistrationOtpInputPort;
import com.aionn.identity.infrastructure.config.properties.AuthProperties;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.adapter.web.support.clientip.ClientIpArgumentResolver;
import com.aionn.sharedkernel.infrastructure.web.ClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerWebTest {

    @Mock
    private InitiateRegistrationInputPort initiateRegistrationInputPort;
    @Mock
    private VerifyRegistrationOtpInputPort verifyRegistrationOtpInputPort;
    @Mock
    private CompleteRegistrationInputPort completeRegistrationInputPort;
    @Mock
    private ResendRegistrationOtpInputPort resendRegistrationOtpInputPort;
    @Mock
    private RegistrationDtoMapper registrationDtoMapper;
    @Mock
    private AuthTokenResponseHandler authTokenResponseHandler;
    @Mock
    private AuthProperties authProperties;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RegistrationController controller = new RegistrationController(
                initiateRegistrationInputPort,
                verifyRegistrationOtpInputPort,
                completeRegistrationInputPort,
                resendRegistrationOtpInputPort,
                registrationDtoMapper,
                authTokenResponseHandler);

        lenient().when(authProperties.clientTypeHeader()).thenReturn("X-Client-Type");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .setCustomArgumentResolvers(
                        new ClientIpArgumentResolver(new ClientIpResolver()),
                        new ClientUserAgentArgumentResolver(),
                        new AuthClientTypeArgumentResolver(authProperties))
                .build();
    }

    @Test
    void initiateRegistrationResolvesClientIpAndReturnsCreatedPayload() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        InitiateRegistrationResult result = new InitiateRegistrationResult(
                "reg-1",
                now.plusSeconds(60),
                now.plusMinutes(5),
                "123456");
        RegistrationSessionResponse response = new RegistrationSessionResponse(
                "reg-1",
                result.resendAvailableAt(),
                result.expiredAt());

        when(registrationDtoMapper.toInitiateCommand(any(InitiateRegistrationRequest.class), anyString()))
                .thenReturn(new InitiateRegistrationCommand("0912345678", "captcha-ok", "203.0.113.10"));
        when(initiateRegistrationInputPort.execute(any())).thenReturn(result);
        when(registrationDtoMapper.toInitiateResponse(result)).thenReturn(response);

        mockMvc.perform(post("/api/v1/registrations/initiate")
                        .contentType(APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.10")
                        .content("""
                                {
                                  "phoneNumber": "0912345678",
                                  "captchaToken": "captcha-ok"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("201"))
                .andExpect(jsonPath("$.message").value("Registration initiated successfully!"))
                .andExpect(jsonPath("$.data.regId").value("reg-1"))
                .andExpect(jsonPath("$.data.otpCode").doesNotExist());

        verify(registrationDtoMapper).toInitiateCommand(
                eq(new InitiateRegistrationRequest("0912345678", "captcha-ok")),
                anyString());
    }

    @Test
    void completeRegistrationResolvesClientContextBeforeDelegating() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        CompleteRegistrationResult result = new CompleteRegistrationResult(
                "user-1",
                "session-1",
                "refresh-1",
                "access-1",
                now.plusMinutes(15),
                now.plusDays(7));
        AuthTokenResponse authTokenResponse = new AuthTokenResponse(
                result.userId(),
                result.sessionId(),
                result.refreshToken(),
                result.accessToken(),
                result.expiresAt(),
                result.sessionExpiresAt());
        ResponseEntity<ApiResponse<AuthTokenResponse>> httpResponse = ResponseEntity.ok(
                ApiResponse.success(authTokenResponse, "Registration completed!"));

        when(registrationDtoMapper.toCompleteCommand(
                eq("reg-1"),
                any(CompleteRegistrationRequest.class),
                anyString(),
                eq("JUnit/1.0")))
                .thenReturn(new CompleteRegistrationCommand(
                        "reg-1",
                        "Password1!",
                        "alice",
                        "verify-token",
                        "198.51.100.20",
                        "JUnit/1.0"));
        when(completeRegistrationInputPort.execute(any())).thenReturn(result);
        when(registrationDtoMapper.toAuthTokenResponse(result)).thenReturn(authTokenResponse);
        when(authTokenResponseHandler.success(authTokenResponse, "mobile", "Registration completed!"))
                .thenReturn(httpResponse);

        mockMvc.perform(post("/api/v1/registrations/reg-1/complete")
                        .contentType(APPLICATION_JSON)
                        .header("X-Forwarded-For", "198.51.100.20")
                        .header("User-Agent", "JUnit/1.0")
                        .header("X-Client-Type", "mobile")
                        .content("""
                                {
                                  "password": "Password1!",
                                  "username": "alice",
                                  "verificationToken": "verify-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.message").value("Registration completed!"))
                .andExpect(jsonPath("$.data.userId").value("user-1"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-1"));

        verify(registrationDtoMapper).toCompleteCommand(
                eq("reg-1"),
                eq(new CompleteRegistrationRequest("Password1!", "alice", "verify-token")),
                anyString(),
                eq("JUnit/1.0"));
        verify(authTokenResponseHandler).success(authTokenResponse, "mobile", "Registration completed!");
    }
}
