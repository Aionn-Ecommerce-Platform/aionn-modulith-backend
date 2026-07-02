package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.consent.request.MarketingConsentRequest;
import com.aionn.identity.adapter.rest.dto.consent.request.TermsConsentRequest;
import com.aionn.identity.adapter.rest.dto.consent.response.ConsentResponse;
import com.aionn.identity.adapter.rest.mapper.consent.ConsentDtoMapper;
import com.aionn.sharedkernel.adapter.web.support.clientip.ClientIp;
import com.aionn.identity.application.port.in.consent.AgreePrivacyInputPort;
import com.aionn.identity.application.port.in.consent.AgreeTermsInputPort;
import com.aionn.identity.application.port.in.consent.GetMyConsentsQueryPort;
import com.aionn.identity.application.port.in.consent.UpdateMarketingConsentInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
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
@RequestMapping("/api/v1/consents")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Identity - Consent", description = "Identity module: terms, privacy, and marketing consent endpoints")
public class ConsentController {

	private final GetMyConsentsQueryPort getMyConsentsQueryPort;
	private final AgreeTermsInputPort agreeTermsInputPort;
	private final AgreePrivacyInputPort agreePrivacyInputPort;
	private final UpdateMarketingConsentInputPort updateMarketingConsentInputPort;
	private final ConsentDtoMapper consentDtoMapper;

	@GetMapping
	@Operation(summary = "Get my consents", description = "Get terms, privacy, and marketing consent records for the authenticated user")
	public ResponseEntity<ApiResponse<List<ConsentResponse>>> getMyConsents(Authentication authentication) {
		var result = getMyConsentsQueryPort.execute(authentication.getName());
		var response = consentDtoMapper.toResponses(result);
		return ResponseEntity.ok(ApiResponse.success(response, "Consents fetched"));
	}

	@PostMapping("/terms")
	@Operation(summary = "Agree terms", description = "Record terms of service consent for the authenticated user")
	public ResponseEntity<ApiResponse<ConsentResponse>> agreeTerms(
			Authentication authentication,
			@ClientIp String clientIp,
			@Valid @RequestBody TermsConsentRequest request) {
		var result = agreeTermsInputPort.execute(
				consentDtoMapper.toTermsConsentCommand(authentication.getName(), clientIp, request));
		var response = consentDtoMapper.toResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "Terms consent recorded"));
	}

	@PostMapping("/privacy")
	@Operation(summary = "Agree privacy policy", description = "Record privacy policy consent for the authenticated user")
	public ResponseEntity<ApiResponse<ConsentResponse>> agreePrivacy(
			Authentication authentication,
			@ClientIp String clientIp,
			@Valid @RequestBody TermsConsentRequest request) {
		var result = agreePrivacyInputPort
				.execute(consentDtoMapper.toPrivacyConsentCommand(authentication.getName(), clientIp, request));
		var response = consentDtoMapper.toResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "Privacy consent recorded"));
	}

	@PatchMapping("/marketing")
	@Operation(summary = "Update marketing consent", description = "Enable or disable marketing consent for the authenticated user")
	public ResponseEntity<ApiResponse<ConsentResponse>> updateMarketing(
			Authentication authentication,
			@ClientIp String clientIp,
			@Valid @RequestBody MarketingConsentRequest request) {
		var result = updateMarketingConsentInputPort
				.execute(consentDtoMapper.toMarketingConsentCommand(authentication.getName(), clientIp, request));
		var response = consentDtoMapper.toResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "Marketing consent updated"));
	}
}
