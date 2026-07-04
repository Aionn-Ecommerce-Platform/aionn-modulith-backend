package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.security.request.ChangePasswordRequest;
import com.aionn.identity.adapter.rest.dto.security.request.CompletePasswordResetRequest;
import com.aionn.identity.adapter.rest.dto.security.request.MfaSetupRequest;
import com.aionn.identity.adapter.rest.dto.security.request.MfaToggleRequest;
import com.aionn.identity.adapter.rest.dto.security.request.PasswordResetRequestCommand;
import com.aionn.identity.adapter.rest.dto.security.response.BackupCodesResponse;
import com.aionn.identity.adapter.rest.dto.security.response.MfaResponse;
import com.aionn.identity.adapter.rest.dto.security.response.MfaSetupResponse;
import com.aionn.identity.adapter.rest.dto.security.response.PasswordResetResponse;
import com.aionn.identity.adapter.rest.dto.security.response.SecurityAuditLogResponse;
import com.aionn.identity.adapter.rest.mapper.security.SecurityDtoMapper;
import com.aionn.identity.adapter.rest.support.response.NoStoreResponseFactory;
import com.aionn.identity.application.port.in.security.*;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.adapter.web.support.clientip.ClientIp;
import com.aionn.sharedkernel.adapter.web.support.idempotency.IdempotentRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
@Tag(name = "Identity - Security", description = "Identity module: password, MFA, audit log endpoints")
public class SecurityController {

	private final ChangePasswordInputPort changePasswordInputPort;
	private final RequestPasswordResetInputPort requestPasswordResetInputPort;
	private final CompletePasswordResetInputPort completePasswordResetInputPort;
	private final InitiateMfaSetupInputPort initiateMfaSetupInputPort;
	private final EnableMfaInputPort enableMfaInputPort;
	private final DisableMfaInputPort disableMfaInputPort;
	private final RegenerateBackupCodesInputPort regenerateBackupCodesInputPort;
	private final GetSecurityAuditLogsQueryPort getSecurityAuditLogsQueryPort;
	private final SecurityDtoMapper securityDtoMapper;
	private final NoStoreResponseFactory noStoreResponseFactory;

	@PutMapping("/password")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Change password", description = "Change password for the authenticated user")
	public ResponseEntity<ApiResponse<Void>> changePassword(
			Authentication authentication,
			@ClientIp String clientIp,
			@Valid @RequestBody ChangePasswordRequest request) {
		changePasswordInputPort
				.execute(securityDtoMapper.toChangePasswordCommand(authentication.getName(), clientIp, request));
		return ResponseEntity.ok(ApiResponse.success("Password changed"));
	}

	@PostMapping("/password-reset-requests")
	@IdempotentRequest(ttlSeconds = 300)
	@Operation(summary = "Request password reset", description = "Create a password reset request for an account")
	public ResponseEntity<ApiResponse<PasswordResetResponse>> requestPasswordReset(
			@ClientIp String clientIp,
			@Valid @RequestBody PasswordResetRequestCommand request) {
		var result = requestPasswordResetInputPort.execute(securityDtoMapper.toPasswordResetCommand(clientIp, request));
		return ResponseEntity.ok(
				ApiResponse.success(securityDtoMapper.toPasswordResetResponse(result), "Password reset requested"));
	}

	@PostMapping("/password-reset")
	@IdempotentRequest(ttlSeconds = 300)
	@Operation(summary = "Complete password reset", description = "Complete password reset using reset token and new password")
	public ResponseEntity<ApiResponse<Void>> completePasswordReset(
			@ClientIp String clientIp,
			@Valid @RequestBody CompletePasswordResetRequest request) {
		completePasswordResetInputPort.execute(securityDtoMapper.toCompletePasswordResetCommand(clientIp, request));
		return ResponseEntity.ok(ApiResponse.success("Password reset completed"));
	}

	@PostMapping("/mfa/setup")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Start MFA setup", description = "Generate a TOTP secret and otpauth URI for the authenticated user")
	public ResponseEntity<ApiResponse<MfaSetupResponse>> setupMfa(
			Authentication authentication,
			@ClientIp String clientIp,
			@Valid @RequestBody MfaSetupRequest request) {
		var result = initiateMfaSetupInputPort
				.execute(securityDtoMapper.toInitiateMfaSetupCommand(authentication.getName(), clientIp, request));
		return noStoreResponseFactory
				.ok(ApiResponse.success(securityDtoMapper.toMfaSetupResponse(result), "MFA setup initiated"));
	}

	@PostMapping("/mfa/enable")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Enable MFA", description = "Confirm TOTP setup and enable multi-factor authentication for the authenticated user")
	public ResponseEntity<ApiResponse<MfaResponse>> enableMfa(
			Authentication authentication,
			@ClientIp String clientIp,
			@Valid @RequestBody MfaToggleRequest request) {
		var result = enableMfaInputPort
				.execute(securityDtoMapper.toEnableMfaCommand(authentication.getName(), clientIp, request));
		return noStoreResponseFactory.ok(ApiResponse.success(securityDtoMapper.toMfaResponse(result), "MFA enabled"));
	}

	@PostMapping("/mfa/disable")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Disable MFA", description = "Disable multi-factor authentication for the authenticated user")
	public ResponseEntity<ApiResponse<MfaResponse>> disableMfa(
			Authentication authentication,
			@ClientIp String clientIp,
			@Valid @RequestBody MfaToggleRequest request) {
		var result = disableMfaInputPort
				.execute(securityDtoMapper.toDisableMfaCommand(authentication.getName(), clientIp, request));
		return noStoreResponseFactory.ok(ApiResponse.success(securityDtoMapper.toMfaResponse(result), "MFA disabled"));
	}

	@PostMapping("/mfa/backup-codes")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Regenerate backup codes", description = "Regenerate MFA backup codes for the authenticated user")
	public ResponseEntity<ApiResponse<BackupCodesResponse>> regenerateBackupCodes(
			Authentication authentication,
			@ClientIp String clientIp,
			@Valid @RequestBody MfaToggleRequest request) {
		var result = regenerateBackupCodesInputPort
				.execute(securityDtoMapper.toRegenerateBackupCodesCommand(
						authentication.getName(), request.password(), request.mfaCode(), clientIp));
		return noStoreResponseFactory.ok(
				ApiResponse.success(securityDtoMapper.toBackupCodesResponse(result), "Backup codes regenerated"));
	}

	@GetMapping("/audit-logs")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Get security audit logs", description = "Get security-related audit logs for the authenticated user")
	public ResponseEntity<ApiResponse<List<SecurityAuditLogResponse>>> getAuditLogs(Authentication authentication) {
		var result = getSecurityAuditLogsQueryPort.execute(authentication.getName());
		// Audit rows carry IP addresses and device identifiers; keep parity with
		// the rest of this controller and mark them uncacheable.
		return noStoreResponseFactory.ok(
				ApiResponse.success(securityDtoMapper.toAuditLogResponse(result), "Security audit logs"));
	}
}
