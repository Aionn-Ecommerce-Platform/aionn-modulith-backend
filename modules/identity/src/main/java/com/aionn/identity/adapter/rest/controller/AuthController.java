package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.auth.request.LoginRequest;
import com.aionn.identity.adapter.rest.dto.auth.request.RefreshTokenRequest;
import com.aionn.identity.adapter.rest.dto.auth.request.SocialAuthRequest;
import com.aionn.identity.adapter.rest.dto.auth.response.AuthSessionResponse;
import com.aionn.identity.adapter.rest.dto.auth.response.AuthTokenResponse;
import com.aionn.identity.adapter.rest.dto.auth.response.LogoutAllResponse;
import com.aionn.identity.adapter.rest.mapper.auth.AuthDtoMapper;
import com.aionn.identity.adapter.rest.support.client.AuthClientType;
import com.aionn.identity.adapter.rest.support.client.ClientUserAgent;
import com.aionn.identity.adapter.rest.support.response.AuthTokenResponseHandler;
import com.aionn.identity.adapter.rest.support.session.CurrentAccessTokenJti;
import com.aionn.identity.adapter.rest.support.session.CurrentSessionId;
import com.aionn.identity.application.port.in.auth.*;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.adapter.web.support.clientip.ClientIp;
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
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Identity - Auth", description = "Identity module: credential login and session endpoints")
public class AuthController {

	private final LoginInputPort loginInputPort;
	private final SocialLoginInputPort socialLoginInputPort;
	private final RefreshTokenInputPort refreshTokenInputPort;
	private final GetAuthSessionsQueryPort getAuthSessionsQueryPort;
	private final RevokeSessionInputPort revokeSessionInputPort;
	private final LogoutAllInputPort logoutAllInputPort;
	private final LogoutInputPort logoutInputPort;
	private final AuthDtoMapper authDtoMapper;
	private final AuthTokenResponseHandler authTokenResponseHandler;

	@PostMapping("/login")
	@Operation(summary = "Login", description = "Authenticate user credentials and create a new auth session")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
			@Valid @RequestBody LoginRequest request,
			@ClientIp String clientIp,
			@ClientUserAgent String userAgent,
			@AuthClientType String clientType) {
		var result = loginInputPort.execute(authDtoMapper.toLoginCommand(request, clientIp, userAgent));
		AuthTokenResponse response = authDtoMapper.toAuthTokenResponse(result);
		return authTokenResponseHandler.success(response, clientType, "Login successful!");
	}

	@PostMapping("/social-login")
	@Operation(summary = "Social login", description = "Authenticate via a verified social provider token")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> socialLogin(
			@Valid @RequestBody SocialAuthRequest request,
			@ClientIp String clientIp,
			@ClientUserAgent String userAgent,
			@AuthClientType String clientType) {
		var result = socialLoginInputPort.execute(authDtoMapper.toSocialLoginCommand(request, clientIp, userAgent));
		AuthTokenResponse response = authDtoMapper.toAuthTokenResponse(result);
		return authTokenResponseHandler.success(response, clientType, "Social login successful!");
	}

	@PostMapping("/refresh")
	@Operation(summary = "Refresh token", description = "Refresh access token using refresh token from body or cookie")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> refreshToken(
			@RequestBody(required = false) RefreshTokenRequest request,
			@CookieValue(name = "refresh_token", required = false) String cookieToken,
			@ClientIp String clientIp,
			@ClientUserAgent String userAgent,
			@AuthClientType String clientType) {
		var result = refreshTokenInputPort
				.execute(authDtoMapper.toRefreshCommand(request, cookieToken, clientIp, userAgent));
		AuthTokenResponse response = authDtoMapper.toAuthTokenResponse(result);
		return authTokenResponseHandler.success(response, clientType, "Token refreshed successfully!");
	}

	@GetMapping("/sessions")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "List sessions", description = "Get active auth sessions for the authenticated user")
	public ResponseEntity<ApiResponse<List<AuthSessionResponse>>> getSessions(
			Authentication authentication) {
		var query = authDtoMapper.toGetSessionsQuery(authentication.getName());
		var result = getAuthSessionsQueryPort.execute(query);
		List<AuthSessionResponse> sessions = authDtoMapper.toAuthSessionResponses(result);
		return ResponseEntity.ok(ApiResponse.success(sessions, "Sessions fetched successfully!"));
	}

	@DeleteMapping("/sessions/{sessionId}")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Revoke session", description = "Revoke one auth session by session ID")
	public ResponseEntity<ApiResponse<Void>> revokeSession(
			Authentication authentication,
			@PathVariable String sessionId) {
		var command = authDtoMapper.toRevokeSessionCommand(authentication.getName(), sessionId);
		revokeSessionInputPort.execute(command);
		return ResponseEntity.ok(ApiResponse.success("Session revoked"));
	}

	@PostMapping("/logout")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Logout", description = "Logout current user from the current session")
	public ResponseEntity<ApiResponse<Void>> logout(
			Authentication authentication,
			@CurrentSessionId String sessionId,
			@CurrentAccessTokenJti String jti) {
		if (sessionId == null) {
			return authTokenResponseHandler.logoutSuccess("Already logged out");
		}
		var command = authDtoMapper.toLogoutCommand(authentication.getName(), sessionId, jti);
		logoutInputPort.execute(command);
		return authTokenResponseHandler.logoutSuccess("Logout successful");
	}

	@PostMapping("/logout-all")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Logout all sessions", description = "Revoke all active sessions for the authenticated user")
	public ResponseEntity<ApiResponse<LogoutAllResponse>> logoutAll(
			Authentication authentication) {
		var command = authDtoMapper.toLogoutAllCommand(authentication.getName());
		var result = logoutAllInputPort.execute(command);
		LogoutAllResponse response = authDtoMapper.toLogoutAllResponse(result);
		return authTokenResponseHandler.logoutAllSuccess(response);
	}
}
