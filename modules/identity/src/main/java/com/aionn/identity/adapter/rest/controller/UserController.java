package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.user.request.ChangeAvatarRequest;
import com.aionn.identity.adapter.rest.dto.user.request.ChangeDisplayNameRequest;
import com.aionn.identity.adapter.rest.dto.user.request.ConfirmOtpRequest;
import com.aionn.identity.adapter.rest.dto.user.request.RequestEmailChangeOtpRequest;
import com.aionn.identity.adapter.rest.dto.user.request.RequestPhoneChangeOtpRequest;
import com.aionn.identity.adapter.rest.dto.user.response.DataExportRequestResponse;
import com.aionn.identity.adapter.rest.dto.user.response.DeletionRequestResponse;
import com.aionn.identity.adapter.rest.dto.user.response.UserProfileResponse;
import com.aionn.identity.adapter.rest.mapper.user.UserDtoMapper;
import com.aionn.identity.application.port.in.user.*;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.adapter.web.support.idempotency.IdempotentRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Identity - User Self-Service", description = "Identity module: current user profile and account self-service endpoints")
public class UserController {

	private final GetMyProfileInputPort getMyProfileInputPort;
	private final VerifyEmailInputPort verifyEmailInputPort;
	private final UpdateDisplayNameInputPort updateDisplayNameInputPort;
	private final UpdateAvatarInputPort updateAvatarInputPort;
	private final ChangeEmailInputPort changeEmailInputPort;
	private final ChangePhoneInputPort changePhoneInputPort;
	private final RequestAccountDeletionInputPort requestAccountDeletionInputPort;
	private final CancelAccountDeletionInputPort cancelAccountDeletionInputPort;
	private final RequestDataExportInputPort requestDataExportInputPort;
	private final UserDtoMapper userDtoMapper;

	@GetMapping
	@Operation(summary = "Get my profile", description = "Get profile information for the authenticated user")
	public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(Authentication authentication) {
		var result = getMyProfileInputPort.execute(userDtoMapper.toGetMyProfileQuery(authentication.getName()));
		UserProfileResponse response = userDtoMapper.toProfileResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "Fetched user profile"));
	}

	@PostMapping("/verify-email/otp")
	@Operation(summary = "Send verification email OTP", description = "Send OTP to the current primary email for verification")
	public ResponseEntity<ApiResponse<Void>> sendVerifyEmailOtp(Authentication authentication) {
		verifyEmailInputPort.sendOtp(authentication.getName());
		return ResponseEntity.ok(ApiResponse.success("Verification OTP sent to email"));
	}

	@PostMapping("/verify-email/confirm")
	@Operation(summary = "Confirm email verification OTP", description = "Confirm the OTP sent to the current primary email")
	public ResponseEntity<ApiResponse<Void>> confirmVerifyEmailOtp(
			Authentication authentication,
			@Valid @RequestBody ConfirmOtpRequest request) {
		verifyEmailInputPort.confirm(authentication.getName(), request.otpCode());
		return ResponseEntity.ok(ApiResponse.success("Email verified"));
	}

	@PatchMapping("/display-name")
	@Operation(summary = "Update display name", description = "Update display name for the authenticated user")
	public ResponseEntity<ApiResponse<UserProfileResponse>> updateDisplayName(
			Authentication authentication,
			@Valid @RequestBody ChangeDisplayNameRequest request) {
		var result = updateDisplayNameInputPort
				.execute(userDtoMapper.toUpdateDisplayNameCommand(authentication.getName(), request));
		UserProfileResponse response = userDtoMapper.toProfileResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "Display name updated"));
	}

	@PatchMapping("/avatar")
	@Operation(summary = "Update avatar", description = "Update avatar URL or avatar reference for the authenticated user")
	public ResponseEntity<ApiResponse<UserProfileResponse>> updateAvatar(
			Authentication authentication,
			@Valid @RequestBody ChangeAvatarRequest request) {
		var result = updateAvatarInputPort
				.execute(userDtoMapper.toUpdateAvatarCommand(authentication.getName(), request));
		UserProfileResponse response = userDtoMapper.toProfileResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "Avatar updated"));
	}

	@PostMapping("/email-change/otp")
	@Operation(summary = "Request email change OTP", description = "Send OTP to the new email before changing the authenticated user's email")
	public ResponseEntity<ApiResponse<Void>> requestEmailChangeOtp(
			Authentication authentication,
			@Valid @RequestBody RequestEmailChangeOtpRequest request) {
		changeEmailInputPort.sendOtp(authentication.getName(), request.newEmail());
		return ResponseEntity.ok(ApiResponse.success("OTP sent to new email"));
	}

	@PostMapping("/email-change/confirm")
	@Operation(summary = "Confirm email change OTP", description = "Confirm the OTP sent to the new email and update the authenticated user's email")
	public ResponseEntity<ApiResponse<UserProfileResponse>> confirmEmailChange(
			Authentication authentication,
			@Valid @RequestBody ConfirmOtpRequest request) {
		var result = changeEmailInputPort.confirm(authentication.getName(), request.otpCode());
		var response = userDtoMapper.toProfileResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "Email updated"));
	}

	@PostMapping("/phone-change/otp")
	@Operation(summary = "Request phone change OTP", description = "Send OTP to the new phone before changing the authenticated user's phone")
	public ResponseEntity<ApiResponse<Void>> requestPhoneChangeOtp(
			Authentication authentication,
			@Valid @RequestBody RequestPhoneChangeOtpRequest request) {
		changePhoneInputPort.sendOtp(authentication.getName(), request.newPhone());
		return ResponseEntity.ok(ApiResponse.success("OTP sent to new phone"));
	}

	@PostMapping("/phone-change/confirm")
	@Operation(summary = "Confirm phone change OTP", description = "Confirm the OTP sent to the new phone and update the authenticated user's phone")
	public ResponseEntity<ApiResponse<UserProfileResponse>> confirmPhoneChange(
			Authentication authentication,
			@Valid @RequestBody ConfirmOtpRequest request) {
		var result = changePhoneInputPort.confirm(authentication.getName(), request.otpCode());
		var response = userDtoMapper.toProfileResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "Phone updated"));
	}

	@PostMapping("/deletion-requests")
	@IdempotentRequest(ttlSeconds = 300)
	@Operation(summary = "Request account deletion", description = "Create account deletion request for the authenticated user")
	public ResponseEntity<ApiResponse<DeletionRequestResponse>> requestAccountDeletion(Authentication authentication) {
		var result = requestAccountDeletionInputPort
				.execute(userDtoMapper.toRequestAccountDeletionCommand(authentication.getName()));
		DeletionRequestResponse response = userDtoMapper.toDeletionRequestResponse(result);
		return ApiResponse.createdResponse("Account deletion requested", response);
	}

	@DeleteMapping("/deletion-requests")
	@Operation(summary = "Cancel account deletion", description = "Cancel existing account deletion request for the authenticated user")
	public ResponseEntity<ApiResponse<Void>> cancelAccountDeletion(Authentication authentication) {
		cancelAccountDeletionInputPort.execute(userDtoMapper.toCancelAccountDeletionCommand(authentication.getName()));
		return ResponseEntity.ok(ApiResponse.success("Account deletion cancelled"));
	}

	@PostMapping("/data-exports")
	@IdempotentRequest(ttlSeconds = 300)
	@Operation(summary = "Request data export", description = "Create personal data export request for the authenticated user")
	public ResponseEntity<ApiResponse<DataExportRequestResponse>> requestDataExport(Authentication authentication) {
		var result = requestDataExportInputPort
				.execute(userDtoMapper.toRequestDataExportCommand(authentication.getName()));
		DataExportRequestResponse response = userDtoMapper.toDataExportResponse(result);
		return ApiResponse.createdResponse("Data export requested", response);
	}
}
