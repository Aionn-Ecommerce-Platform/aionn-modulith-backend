package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.admin.result.UserDetailResult;
import com.aionn.identity.application.dto.admin.result.UserListResult;
import com.aionn.identity.application.dto.common.PageResult;
import com.aionn.identity.application.mapper.AdminResultMapper;
import com.aionn.identity.application.port.out.admin.AdminUserPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserService {

    private final AdminUserPersistencePort adminUserPersistencePort;
    private final AdminResultMapper adminResultMapper;

    public Set<String> updateRoles(String userId, Set<UserRole> roles) {
        log.info("Updating roles for user: {}", userId);
        IdentityUser user = getUser(userId);
        user.setRoles(requireRoles(roles));
        IdentityUser saved = adminUserPersistencePort.save(user);
        return toRoleNames(saved);
    }

    public Set<String> removeRoles(String userId, Set<UserRole> roles) {
        log.info("Removing roles from user: {}", userId);
        IdentityUser user = getUser(userId);
        Set<UserRole> toRemove = requireRoles(roles);
        Set<UserRole> remaining = user.getRoles().stream()
                .filter(existing -> !toRemove.contains(existing))
                .collect(Collectors.toSet());
        if (remaining.isEmpty()) {
            remaining.add(UserRole.BUYER);
        }
        user.setRoles(remaining);
        IdentityUser saved = adminUserPersistencePort.save(user);
        return toRoleNames(saved);
    }

    public String updateStatus(String userId, UserStatus status) {
        log.info("Updating status for user: {} to {}", userId, status);
        IdentityUser user = getUser(userId);
        user.updateStatus(requireStatus(status));
        IdentityUser saved = adminUserPersistencePort.save(user);
        return saved.getStatus().name();
    }

    public void unlockAccount(String userId) {
        log.info("Unlocking account for user: {}", userId);
        IdentityUser user = getUser(userId);
        user.unlock();
        adminUserPersistencePort.save(user);
    }

    @Transactional(readOnly = true)
    public UserListResult listUsers(UserStatus status, UserRole role, int page, int size) {
        OffsetPagination pagination = OffsetPagination.safe(page, size);

        log.debug("Listing users status={}, role={}, page={}, size={}", status, role,
                pagination.page(), pagination.size());

        PageResult<IdentityUser> userPage = adminUserPersistencePort.findUsersWithFilters(
                status, role, pagination);

        var users = userPage.content().stream()
                .map(user -> new UserListResult.UserSummary(
                        user.getUserId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getStatus() != null ? user.getStatus().name() : null,
                        user.getRoles().stream()
                                .map(Enum::name)
                                .sorted()
                                .collect(Collectors.joining(","))))
                .toList();

        return adminResultMapper.toUserListResult(users, pagination.page(), pagination.size(),
                (int) userPage.totalElements());
    }

    @Transactional(readOnly = true)
    public UserDetailResult getUserById(String userId) {
        return adminResultMapper.toUserDetailResult(getUser(userId));
    }

    private IdentityUser getUser(String userId) {
        return adminUserPersistencePort.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
    }

    private static Set<String> toRoleNames(IdentityUser user) {
        return user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    private static Set<UserRole> requireRoles(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IdentityException(IdentityErrorCode.INVALID_USER_ROLE,
                    "At least one role must be supplied");
        }
        if (roles.stream().anyMatch(Objects::isNull)) {
            throw new IdentityException(IdentityErrorCode.INVALID_USER_ROLE,
                    "Roles must not contain null values");
        }
        return roles;
    }

    private static UserStatus requireStatus(UserStatus status) {
        if (status == null) {
            throw new IdentityException(IdentityErrorCode.INVALID_USER_STATUS,
                    "User status must be supplied");
        }
        return status;
    }
}
