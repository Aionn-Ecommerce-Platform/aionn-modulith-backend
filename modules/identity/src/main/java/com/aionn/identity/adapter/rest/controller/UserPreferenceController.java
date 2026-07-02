package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.preference.request.AiPrivacyPreferenceRequest;
import com.aionn.identity.adapter.rest.dto.preference.request.GeneralPreferenceRequest;
import com.aionn.identity.adapter.rest.dto.preference.request.NotificationPreferenceRequest;
import com.aionn.identity.adapter.rest.dto.preference.response.UserPreferenceResponse;
import com.aionn.identity.adapter.rest.mapper.preference.UserPreferenceDtoMapper;
import com.aionn.identity.application.port.in.preference.GetUserPreferenceQueryPort;
import com.aionn.identity.application.port.in.preference.UpdateAiPrivacyPreferenceInputPort;
import com.aionn.identity.application.port.in.preference.UpdateGeneralPreferenceInputPort;
import com.aionn.identity.application.port.in.preference.UpdateNotificationPreferenceInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Identity - Preferences", description = "Identity module: personal preference and privacy configuration endpoints")
public class UserPreferenceController {

	private final GetUserPreferenceQueryPort getUserPreferenceQueryPort;
	private final UpdateGeneralPreferenceInputPort updateGeneralPreferenceInputPort;
	private final UpdateNotificationPreferenceInputPort updateNotificationPreferenceInputPort;
	private final UpdateAiPrivacyPreferenceInputPort updateAiPrivacyPreferenceInputPort;
	private final UserPreferenceDtoMapper userPreferenceDtoMapper;

	@GetMapping
	@Operation(summary = "Get user preferences", description = "Get all preference settings for the authenticated user")
	public ResponseEntity<ApiResponse<UserPreferenceResponse>> getPreferences(Authentication authentication) {
		var result = getUserPreferenceQueryPort.execute(authentication.getName());
		var response = userPreferenceDtoMapper.toResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "Preferences fetched"));
	}

	@PutMapping("/general")
	@Operation(summary = "Update general preferences", description = "Update general preference settings for the authenticated user")
	public ResponseEntity<ApiResponse<UserPreferenceResponse>> updateGeneral(
			Authentication authentication,
			@Valid @RequestBody GeneralPreferenceRequest request) {
		var result = updateGeneralPreferenceInputPort
				.execute(userPreferenceDtoMapper.toUpdateGeneralCommand(authentication.getName(), request));
		var response = userPreferenceDtoMapper.toResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "General preferences updated"));
	}

	@PutMapping("/notifications")
	@Operation(summary = "Update notification preferences", description = "Update notification preference settings for the authenticated user")
	public ResponseEntity<ApiResponse<UserPreferenceResponse>> updateNotifications(
			Authentication authentication,
			@Valid @RequestBody NotificationPreferenceRequest request) {
		var result = updateNotificationPreferenceInputPort
				.execute(userPreferenceDtoMapper.toUpdateNotificationsCommand(authentication.getName(), request));
		var response = userPreferenceDtoMapper.toResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "Notification preferences updated"));
	}

	@PutMapping("/ai-privacy")
	@Operation(summary = "Update AI privacy preferences", description = "Update AI and privacy preference settings for the authenticated user")
	public ResponseEntity<ApiResponse<UserPreferenceResponse>> updateAiPrivacy(
			Authentication authentication,
			@Valid @RequestBody AiPrivacyPreferenceRequest request) {
		var result = updateAiPrivacyPreferenceInputPort
				.execute(userPreferenceDtoMapper.toUpdateAiPrivacyCommand(authentication.getName(), request));
		var response = userPreferenceDtoMapper.toResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "AI privacy preferences updated"));
	}
}


