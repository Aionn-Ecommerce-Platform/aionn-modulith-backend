package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.security.request.*;
import com.aionn.identity.adapter.rest.dto.security.response.*;
import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.adapter.rest.mapper.security.SecurityDtoMapper;
import com.aionn.identity.adapter.rest.support.client.AuthClientTypeArgumentResolver;
import com.aionn.identity.adapter.rest.support.client.ClientUserAgentArgumentResolver;
import com.aionn.identity.adapter.rest.support.response.NoStoreResponseFactory;
import com.aionn.identity.adapter.rest.support.MockAuthenticationArgumentResolver;
import com.aionn.identity.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.identity.application.dto.security.command.*;
import com.aionn.identity.application.dto.security.result.*;
import com.aionn.identity.application.port.in.security.*;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
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
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
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
class SecurityControllerWebTest {

  @Mock
  private ChangePasswordInputPort changePasswordInputPort;
  @Mock
  private RequestPasswordResetInputPort requestPasswordResetInputPort;
  @Mock
  private CompletePasswordResetInputPort completePasswordResetInputPort;
  @Mock
  private InitiateMfaSetupInputPort initiateMfaSetupInputPort;
  @Mock
  private EnableMfaInputPort enableMfaInputPort;
  @Mock
  private DisableMfaInputPort disableMfaInputPort;
  @Mock
  private RegenerateBackupCodesInputPort regenerateBackupCodesInputPort;
  @Mock
  private GetSecurityAuditLogsQueryPort getSecurityAuditLogsQueryPort;
  @Mock
  private SecurityDtoMapper securityDtoMapper;
  @Mock
  private NoStoreResponseFactory noStoreResponseFactory;
  @Mock
  private AuthProperties authProperties;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    SecurityController controller = new SecurityController(
        changePasswordInputPort, requestPasswordResetInputPort, completePasswordResetInputPort,
        initiateMfaSetupInputPort, enableMfaInputPort, disableMfaInputPort,
        regenerateBackupCodesInputPort, getSecurityAuditLogsQueryPort,
        securityDtoMapper, noStoreResponseFactory);

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
  void changePasswordSuccessfullyUpdatesPassword() throws Exception {
    when(securityDtoMapper.toChangePasswordCommand(eq("alice@example.com"), anyString(), any()))
        .thenReturn(new ChangePasswordCommand("user-123", "oldPassword123", "newPassword456",
            "192.168.1.1"));
    doNothing().when(changePasswordInputPort).execute(any());

    mockMvc.perform(put("/api/v1/security/password")
        .with(user("alice@example.com").roles("USER"))
        .header("X-Forwarded-For", "192.168.1.1")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "currentPassword": "oldPassword123",
              "newPassword": "newPassword456"
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Password changed"));

    verify(changePasswordInputPort).execute(any());
  }

  @Test
  void requestPasswordResetCreatesResetRequest() throws Exception {
    PasswordResetResult result = PasswordResetResult.acceptedResult();
    PasswordResetResponse response = new PasswordResetResponse("Password reset requested");

    when(securityDtoMapper.toPasswordResetCommand(anyString(),
        any(PasswordResetRequestCommand.class)))
        .thenReturn(new RequestPasswordResetCommand("alice@example.com", "192.168.1.5"));
    when(requestPasswordResetInputPort.execute(any())).thenReturn(result);
    when(securityDtoMapper.toPasswordResetResponse(result)).thenReturn(response);

    mockMvc.perform(post("/api/v1/security/password-reset-requests")
        .header("X-Forwarded-For", "192.168.1.5")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "identity": "alice@example.com"
            }
            """)).andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Password reset requested"));

    verify(requestPasswordResetInputPort).execute(any());
  }

  @Test
  void completePasswordResetUpdatesPasswordWithToken() throws Exception {
    when(securityDtoMapper.toCompletePasswordResetCommand(anyString(),
        any(CompletePasswordResetRequest.class)))
        .thenReturn(new CompletePasswordResetCommand("reset-token-xyz", "newPassword789",
            "10.0.0.1"));
    doNothing().when(completePasswordResetInputPort).execute(any());

    mockMvc.perform(post("/api/v1/security/password-reset")
        .header("X-Forwarded-For", "10.0.0.1")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "token": "reset-token-xyz",
              "newPassword": "newPassword789"
            }
            """)).andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Password reset completed"));

    verify(completePasswordResetInputPort).execute(any());
  }

  @Test
  void setupMfaInitiatesSetupAndReturnsSecret() throws Exception {
    MfaSetupResult result = new MfaSetupResult("secret-base32", "otpauth://totp/...", "Aionn",
        "alice@example.com");
    MfaSetupResponse response = new MfaSetupResponse("secret-base32", "otpauth://totp/...", "Aionn",
        "alice@example.com");

    when(securityDtoMapper.toInitiateMfaSetupCommand(eq("alice@example.com"), anyString(),
        any(MfaSetupRequest.class)))
        .thenReturn(new InitiateMfaSetupCommand("user-123", "password123", "192.168.1.1"));
    when(initiateMfaSetupInputPort.execute(any())).thenReturn(result);
    when(securityDtoMapper.toMfaSetupResponse(result)).thenReturn(response);
    when(noStoreResponseFactory.ok(any()))
        .thenReturn(ResponseEntity.ok(ApiResponse.success(response, "MFA setup initiated")));

    mockMvc.perform(post("/api/v1/security/mfa/setup")
        .with(user("alice@example.com").roles("USER"))
        .header("X-Forwarded-For", "192.168.1.1")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "password": "password123"
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.secret").value("secret-base32"));

    verify(initiateMfaSetupInputPort).execute(any());
  }

  @Test
  void enableMfaActivatesMfaForUser() throws Exception {
    MfaResult result = new MfaResult(true, List.of("backup1", "backup2", "backup3"));
    MfaResponse response = new MfaResponse(true, List.of("backup1", "backup2", "backup3"));

    when(securityDtoMapper.toEnableMfaCommand(eq("alice@example.com"), anyString(),
        any(MfaToggleRequest.class)))
        .thenReturn(new EnableMfaCommand("user-123", "password123", "123456", "192.168.1.1"));
    when(enableMfaInputPort.execute(any())).thenReturn(result);
    when(securityDtoMapper.toMfaResponse(result)).thenReturn(response);
    when(noStoreResponseFactory.ok(any()))
        .thenReturn(ResponseEntity.ok(ApiResponse.success(response, "MFA enabled")));

    mockMvc.perform(post("/api/v1/security/mfa/enable")
        .with(user("alice@example.com").roles("USER"))
        .header("X-Forwarded-For", "192.168.1.1")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "password": "password123",
              "mfaCode": "123456"
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.mfaEnabled").value(true))
        .andExpect(jsonPath("$.data.backupCodes").isArray());

    verify(enableMfaInputPort).execute(any());
  }

  @Test
  void disableMfaDeactivatesMfaForUser() throws Exception {
    MfaResult result = new MfaResult(false, null);
    MfaResponse response = new MfaResponse(false, null);

    when(securityDtoMapper.toDisableMfaCommand(eq("alice@example.com"), anyString(),
        any(MfaToggleRequest.class)))
        .thenReturn(new DisableMfaCommand("user-123", "password123", "654321", "192.168.1.1"));
    when(disableMfaInputPort.execute(any())).thenReturn(result);
    when(securityDtoMapper.toMfaResponse(result)).thenReturn(response);
    when(noStoreResponseFactory.ok(any()))
        .thenReturn(ResponseEntity.ok(ApiResponse.success(response, "MFA disabled")));

    mockMvc.perform(post("/api/v1/security/mfa/disable")
        .with(user("alice@example.com").roles("USER"))
        .header("X-Forwarded-For", "192.168.1.1")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "password": "password123",
              "mfaCode": "654321"
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.mfaEnabled").value(false));

    verify(disableMfaInputPort).execute(any());
  }

  @Test
  void regenerateBackupCodesGeneratesNewCodes() throws Exception {
    BackupCodesResult result = new BackupCodesResult(List.of("new1", "new2", "new3", "new4", "new5"));
    BackupCodesResponse response = new BackupCodesResponse(List.of("new1", "new2", "new3", "new4", "new5"));

    when(securityDtoMapper.toRegenerateBackupCodesCommand(eq("alice@example.com"), eq("password123"), eq("111222"),
        anyString()))
        .thenReturn(new RegenerateBackupCodesCommand("user-123", "password123", "111222",
            "192.168.1.1"));
    when(regenerateBackupCodesInputPort.execute(any())).thenReturn(result);
    when(securityDtoMapper.toBackupCodesResponse(result)).thenReturn(response);
    when(noStoreResponseFactory.ok(any())).thenReturn(
        ResponseEntity.ok(ApiResponse.success(response, "Backup codes regenerated")));

    mockMvc.perform(post("/api/v1/security/mfa/backup-codes")
        .with(user("alice@example.com").roles("USER"))
        .header("X-Forwarded-For", "192.168.1.1")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "password": "password123",
              "mfaCode": "111222"
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.backupCodes").isArray())
        .andExpect(jsonPath("$.data.backupCodes[0]").value("new1"));

    verify(regenerateBackupCodesInputPort).execute(any());
  }

  @Test
  void getAuditLogsReturnsSecurityAuditLogs() throws Exception {
    LocalDateTime now = LocalDateTime.now();
    SecurityAuditLogResult log1 = new SecurityAuditLogResult("audit-1", "LOGIN", "Login successful",
        "192.168.1.1", "Chrome", now);
    SecurityAuditLogResult log2 = new SecurityAuditLogResult("audit-2", "PASSWORD_CHANGE",
        "Password changed successfully", "10.0.0.5", "Firefox",
        now.minusDays(1));
    List<SecurityAuditLogResult> logs = List.of(log1, log2);

    SecurityAuditLogResponse resp1 = new SecurityAuditLogResponse("audit-1", "LOGIN", "Login successful",
        "192.168.1.1",
        "Chrome",
        now);
    SecurityAuditLogResponse resp2 = new SecurityAuditLogResponse("audit-2", "PASSWORD_CHANGE",
        "Password changed successfully", "10.0.0.5",
        "Firefox", now.minusDays(1));

    when(getSecurityAuditLogsQueryPort.execute("alice@example.com")).thenReturn(logs);
    when(securityDtoMapper.toAuditLogResponse(logs)).thenReturn(List.of(resp1, resp2));

    mockMvc.perform(get("/api/v1/security/audit-logs")
        .with(user("alice@example.com").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].auditId").value("audit-1"))
        .andExpect(jsonPath("$.data[1].eventType").value("PASSWORD_CHANGE"));

    verify(getSecurityAuditLogsQueryPort).execute("alice@example.com");
  }

  @Test
  void changePasswordRejectsBlankCurrentPassword() throws Exception {
    mockMvc.perform(put("/api/v1/security/password")
        .with(user("alice@example.com").roles("USER"))
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "currentPassword": "",
              "newPassword": "newPassword456"
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));

    verifyNoInteractions(changePasswordInputPort);
  }

  @Test
  void changePasswordRejectsTooShortNewPassword() throws Exception {
    mockMvc.perform(put("/api/v1/security/password")
        .with(user("alice@example.com").roles("USER"))
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "currentPassword": "oldPassword123",
              "newPassword": "short"
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));

    verifyNoInteractions(changePasswordInputPort);
  }

  @Test
  void changePasswordWhenInvalidCredentialsReturns401() throws Exception {
    when(securityDtoMapper.toChangePasswordCommand(eq("alice@example.com"), anyString(), any()))
        .thenReturn(new ChangePasswordCommand("user-123", "wrong", "newPassword456",
            "192.168.1.1"));
    doThrow(new IdentityException(IdentityErrorCode.INVALID_CREDENTIALS))
        .when(changePasswordInputPort).execute(any());

    mockMvc.perform(put("/api/v1/security/password")
        .with(user("alice@example.com").roles("USER"))
        .header("X-Forwarded-For", "192.168.1.1")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "currentPassword": "wrong",
              "newPassword": "newPassword456"
            }
            """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_203"));
  }

  @Test
  void completePasswordResetWhenTokenInvalidReturns401() throws Exception {
    when(securityDtoMapper.toCompletePasswordResetCommand(anyString(),
        any(CompletePasswordResetRequest.class)))
        .thenReturn(new CompletePasswordResetCommand("bad-token", "newPassword789",
            "10.0.0.1"));
    doThrow(new IdentityException(IdentityErrorCode.VERIFICATION_TOKEN_INVALID))
        .when(completePasswordResetInputPort).execute(any());

    mockMvc.perform(post("/api/v1/security/password-reset")
        .header("X-Forwarded-For", "10.0.0.1")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "token": "bad-token",
              "newPassword": "newPassword789"
            }
            """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_106"));
  }

  @Test
  void completePasswordResetRejectsBlankToken() throws Exception {
    mockMvc.perform(post("/api/v1/security/password-reset")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "token": "",
              "newPassword": "newPassword789"
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));

    verifyNoInteractions(completePasswordResetInputPort);
  }

  @Test
  void requestPasswordResetRejectsBlankIdentity() throws Exception {
    mockMvc.perform(post("/api/v1/security/password-reset-requests")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "identity": ""
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));

    verifyNoInteractions(requestPasswordResetInputPort);
  }

  @Test
  void setupMfaWhenAlreadyEnabledReturns409() throws Exception {
    when(securityDtoMapper.toInitiateMfaSetupCommand(eq("alice@example.com"), anyString(),
        any(MfaSetupRequest.class)))
        .thenReturn(new InitiateMfaSetupCommand("user-123", "password123", "192.168.1.1"));
    when(initiateMfaSetupInputPort.execute(any()))
        .thenThrow(new IdentityException(IdentityErrorCode.MFA_ALREADY_ENABLED));

    mockMvc.perform(post("/api/v1/security/mfa/setup")
        .with(user("alice@example.com").roles("USER"))
        .header("X-Forwarded-For", "192.168.1.1")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "password": "password123"
            }
            """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_221"));
  }

  @Test
  void setupMfaWhenWrongPasswordReturns401() throws Exception {
    when(securityDtoMapper.toInitiateMfaSetupCommand(eq("alice@example.com"), anyString(),
        any(MfaSetupRequest.class)))
        .thenReturn(new InitiateMfaSetupCommand("user-123", "wrong", "192.168.1.1"));
    when(initiateMfaSetupInputPort.execute(any()))
        .thenThrow(new IdentityException(IdentityErrorCode.INVALID_CREDENTIALS));

    mockMvc.perform(post("/api/v1/security/mfa/setup")
        .with(user("alice@example.com").roles("USER"))
        .header("X-Forwarded-For", "192.168.1.1")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "password": "wrong"
            }
            """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_203"));
  }

  @Test
  void enableMfaRejectsBlankMfaCode() throws Exception {
    mockMvc.perform(post("/api/v1/security/mfa/enable")
        .with(user("alice@example.com").roles("USER"))
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "password": "password123",
              "mfaCode": ""
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));

    verifyNoInteractions(enableMfaInputPort);
  }

  @Test
  void enableMfaWhenSetupNotInitiatedReturns400() throws Exception {
    when(securityDtoMapper.toEnableMfaCommand(eq("alice@example.com"), anyString(),
        any(MfaToggleRequest.class)))
        .thenReturn(new EnableMfaCommand("user-123", "password123", "123456", "192.168.1.1"));
    when(enableMfaInputPort.execute(any()))
        .thenThrow(new IdentityException(IdentityErrorCode.MFA_SETUP_NOT_INITIATED));

    mockMvc.perform(post("/api/v1/security/mfa/enable")
        .with(user("alice@example.com").roles("USER"))
        .header("X-Forwarded-For", "192.168.1.1")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "password": "password123",
              "mfaCode": "123456"
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_223"));
  }

  @Test
  void disableMfaWhenNotEnabledReturns400() throws Exception {
    when(securityDtoMapper.toDisableMfaCommand(eq("alice@example.com"), anyString(),
        any(MfaToggleRequest.class)))
        .thenReturn(new DisableMfaCommand("user-123", "password123", "654321", "192.168.1.1"));
    when(disableMfaInputPort.execute(any()))
        .thenThrow(new IdentityException(IdentityErrorCode.MFA_NOT_ENABLED));

    mockMvc.perform(post("/api/v1/security/mfa/disable")
        .with(user("alice@example.com").roles("USER"))
        .header("X-Forwarded-For", "192.168.1.1")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "password": "password123",
              "mfaCode": "654321"
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_222"));
  }

  @Test
  void regenerateBackupCodesWhenInvalidMfaCodeReturns400() throws Exception {
    when(securityDtoMapper.toRegenerateBackupCodesCommand(eq("alice@example.com"), eq("password123"),
        eq("000000"), anyString()))
        .thenReturn(new RegenerateBackupCodesCommand("user-123", "password123", "000000",
            "192.168.1.1"));
    when(regenerateBackupCodesInputPort.execute(any()))
        .thenThrow(new IdentityException(IdentityErrorCode.OTP_INVALID));

    mockMvc.perform(post("/api/v1/security/mfa/backup-codes")
        .with(user("alice@example.com").roles("USER"))
        .header("X-Forwarded-For", "192.168.1.1")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "password": "password123",
              "mfaCode": "000000"
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_101"));
  }

  @Test
  void unauthorizedRequestToProtectedEndpointReturns401() throws Exception {
    mockMvc.perform(put("/api/v1/security/password")
        .contentType(APPLICATION_JSON)
        .content("""
            {
              "currentPassword": "oldPassword123",
              "newPassword": "newPassword456"
            }
            """))
        .andExpect(status().isUnauthorized());
  }
}
