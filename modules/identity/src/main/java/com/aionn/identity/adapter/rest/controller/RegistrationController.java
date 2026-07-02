package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.auth.response.AuthTokenResponse;
import com.aionn.identity.adapter.rest.dto.registration.request.CompleteRegistrationRequest;
import com.aionn.identity.adapter.rest.dto.registration.request.InitiateRegistrationRequest;
import com.aionn.identity.adapter.rest.dto.registration.request.VerifyOtpRequest;
import com.aionn.identity.adapter.rest.dto.registration.response.RegistrationSessionResponse;
import com.aionn.identity.adapter.rest.dto.registration.response.VerifyOtpResponse;
import com.aionn.identity.adapter.rest.mapper.registration.RegistrationDtoMapper;
import com.aionn.identity.adapter.rest.support.client.AuthClientType;
import com.aionn.identity.adapter.rest.support.client.ClientUserAgent;
import com.aionn.identity.adapter.rest.support.response.AuthTokenResponseHandler;
import com.aionn.identity.application.port.in.registration.CompleteRegistrationInputPort;
import com.aionn.identity.application.port.in.registration.InitiateRegistrationInputPort;
import com.aionn.identity.application.port.in.registration.ResendRegistrationOtpInputPort;
import com.aionn.identity.application.port.in.registration.VerifyRegistrationOtpInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.adapter.web.support.clientip.ClientIp;
import com.aionn.sharedkernel.adapter.web.support.idempotency.IdempotentRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/registrations")
@RequiredArgsConstructor
@Tag(name = "Identity - Registration", description = "Identity module: registration and OTP verification endpoints")
public class RegistrationController {

	private final InitiateRegistrationInputPort initiateRegistrationInputPort;
	private final VerifyRegistrationOtpInputPort verifyRegistrationOtpInputPort;
	private final CompleteRegistrationInputPort completeRegistrationInputPort;
	private final ResendRegistrationOtpInputPort resendRegistrationOtpInputPort;
	private final RegistrationDtoMapper registrationDtoMapper;
	private final AuthTokenResponseHandler authTokenResponseHandler;

	@Operation(summary = "Initiate registration", description = "Start registration and send OTP to user")
	@PostMapping("/initiate")
	@IdempotentRequest(ttlSeconds = 300)
	public ResponseEntity<ApiResponse<RegistrationSessionResponse>> initRegistration(
			@Valid @RequestBody InitiateRegistrationRequest request,
			@ClientIp String ipAddress) {
		var result = initiateRegistrationInputPort.execute(registrationDtoMapper.toInitiateCommand(request, ipAddress));
		RegistrationSessionResponse response = registrationDtoMapper.toInitiateResponse(result);
		return ApiResponse.createdResponse("Registration initiated successfully!", response);
	}

	@Operation(summary = "Verify OTP", description = "Verify OTP for a registration session")
	@PostMapping("/{regId}/verify-otp")
	public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyOtp(
			@PathVariable String regId,
			@Valid @RequestBody VerifyOtpRequest request) {
		var result = verifyRegistrationOtpInputPort.execute(registrationDtoMapper.toVerifyOtpCommand(regId, request));
		VerifyOtpResponse response = registrationDtoMapper.toVerifyOtpResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "OTP verified successfully!"));
	}

	@Operation(summary = "Complete registration", description = "Finalize registration and create user account")
	@PostMapping("/{regId}/complete")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> completeRegistration(
			@PathVariable String regId,
			@Valid @RequestBody CompleteRegistrationRequest request,
			@ClientIp String ipAddress,
			@ClientUserAgent String userAgent,
			@AuthClientType String clientType) {
		var result = completeRegistrationInputPort.execute(
				registrationDtoMapper.toCompleteCommand(regId, request, ipAddress, userAgent));
		AuthTokenResponse response = registrationDtoMapper.toAuthTokenResponse(result);
		return authTokenResponseHandler.success(response, clientType, "Registration completed!");
	}

	@Operation(summary = "Resend OTP", description = "Resend OTP code for a registration session")
	@PostMapping("/{regId}/resend-otp")
	@IdempotentRequest(ttlSeconds = 120)
	public ResponseEntity<ApiResponse<RegistrationSessionResponse>> resendOtp(
			@PathVariable String regId,
			@ClientIp String ipAddress) {
		var result = resendRegistrationOtpInputPort.execute(registrationDtoMapper.toResendOtpCommand(regId, ipAddress));
		RegistrationSessionResponse response = registrationDtoMapper.toResendOtpResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "OTP resent successfully!"));
	}

}
