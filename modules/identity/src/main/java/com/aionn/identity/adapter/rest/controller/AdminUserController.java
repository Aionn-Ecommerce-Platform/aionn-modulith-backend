package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.admin.request.UpdateRolesRequest;
import com.aionn.identity.adapter.rest.dto.admin.request.UpdateUserStatusRequest;
import com.aionn.identity.adapter.rest.dto.admin.response.UserDetailResponse;
import com.aionn.identity.adapter.rest.dto.admin.response.UserRolesResponse;
import com.aionn.identity.adapter.rest.dto.admin.response.UserStatusResponse;
import com.aionn.identity.adapter.rest.dto.admin.response.UserAnalyticsResponse;
import com.aionn.identity.adapter.rest.dto.admin.response.UserSummaryResponse;
import com.aionn.identity.adapter.rest.dto.security.request.UnlockAccountRequest;
import com.aionn.identity.adapter.rest.mapper.admin.AdminUserDtoMapper;
import com.aionn.identity.adapter.rest.mapper.security.SecurityDtoMapper;
import com.aionn.identity.application.port.in.admin.*;
import com.aionn.identity.application.port.in.security.UnlockAccountInputPort;
import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.adapter.web.response.PageMetadata;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
@Tag(name = "Identity - Admin User", description = "Identity module: admin endpoints for user role and status management")
public class AdminUserController {

	private final ListUsersQueryPort listUsersQueryPort;
	private final GetUserByIdQueryPort getUserByIdQueryPort;
	private final UpdateUserRolesInputPort updateUserRolesInputPort;
	private final RemoveUserRolesInputPort removeUserRolesInputPort;
	private final UpdateUserStatusInputPort updateUserStatusInputPort;
	private final UnlockAccountInputPort unlockAccountInputPort;
	private final GetUserAnalyticsQueryPort getUserAnalyticsQueryPort;
	private final AdminUserDtoMapper adminUserDtoMapper;
	private final SecurityDtoMapper securityDtoMapper;

	@GetMapping("/analytics")
	@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
	@Operation(summary = "User signup + role/status analytics (sysadmin)")
	public ResponseEntity<ApiResponse<UserAnalyticsResponse>> analytics(
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		var result = getUserAnalyticsQueryPort.execute(from, to);
		return ResponseEntity.ok(ApiResponse.success(
				adminUserDtoMapper.toUserAnalyticsResponse(result), "User analytics fetched"));
	}

	@GetMapping
	@Operation(summary = "List users", description = "Get paginated users for admin management with optional status and role filters")
	public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> listUsers(
			@RequestParam(required = false) UserStatus status,
			@RequestParam(required = false) UserRole role,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		OffsetPagination safe = OffsetPagination.safe(page, size);
		var result = listUsersQueryPort.execute(
				adminUserDtoMapper.toListUsersQuery(status, role, safe.page(), safe.size()));
		List<UserSummaryResponse> users = adminUserDtoMapper.toUserSummaryResponses(result);
		PageMetadata paging = adminUserDtoMapper.toUserListPaging(result);
		return ResponseEntity.ok(ApiResponse.successWithPaging(users, paging, "Users fetched"));
	}

	@GetMapping("/{userId}")
	@Operation(summary = "Get user detail", description = "Get full user profile details by user ID for admin management")
	public ResponseEntity<ApiResponse<UserDetailResponse>> getUserById(@PathVariable String userId) {
		var result = getUserByIdQueryPort.execute(adminUserDtoMapper.toGetUserQuery(userId));
		return ResponseEntity.ok(ApiResponse.success(adminUserDtoMapper.toUserDetailResponse(result), "User fetched"));
	}

	@PutMapping("/{userId}/roles")
	// Only sysadmin can grant/replace roles — CS admin must not be able to
	// escalate itself or others to SYSTEM_ADMIN.
	@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
	@Operation(summary = "Update user roles", description = "Replace user roles with the provided role set")
	public ResponseEntity<ApiResponse<UserRolesResponse>> updateRoles(
			@PathVariable String userId,
			@Valid @RequestBody UpdateRolesRequest request) {
		var result = updateUserRolesInputPort.execute(adminUserDtoMapper.toUpdateRolesCommand(userId, request));
		return ResponseEntity.ok(ApiResponse.success(adminUserDtoMapper.toRolesResponse(result), "Roles updated"));
	}

	@DeleteMapping("/{userId}/roles")
	@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
	@Operation(summary = "Remove user roles", description = "Remove specific roles from a user account")
	public ResponseEntity<ApiResponse<Void>> removeRoles(
			@PathVariable String userId,
			@Valid @RequestBody UpdateRolesRequest request) {
		removeUserRolesInputPort.execute(adminUserDtoMapper.toRemoveRolesCommand(userId, request));
		return ResponseEntity.ok(ApiResponse.success("Roles removed"));
	}

	@PatchMapping("/{userId}/status")
	@Operation(summary = "Update user status", description = "Change account status for a user")
	public ResponseEntity<ApiResponse<UserStatusResponse>> updateStatus(
			@PathVariable String userId,
			@Valid @RequestBody UpdateUserStatusRequest request) {
		var result = updateUserStatusInputPort.execute(adminUserDtoMapper.toUpdateStatusCommand(userId, request));
		return ResponseEntity.ok(ApiResponse.success(adminUserDtoMapper.toStatusResponse(result), "Status updated"));
	}

	@PostMapping("/unlock")
	@Operation(summary = "Unlock account", description = "Unlock a locked user account by admin")
	public ResponseEntity<ApiResponse<Void>> unlockAccount(@Valid @RequestBody UnlockAccountRequest request) {
		unlockAccountInputPort.execute(securityDtoMapper.toUnlockAccountCommand(request.userId()));
		return ResponseEntity.ok(ApiResponse.success("Account unlocked"));
	}
}
