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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Mock
    private AdminUserPersistencePort adminUserPersistencePort;
    @Mock
    private AdminResultMapper adminResultMapper;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(adminUserPersistencePort, adminResultMapper);
    }

    @Test
    void updateRolesReplacesExistingRoleSet() {
        IdentityUser user = newUser(Set.of(UserRole.BUYER));
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));
        when(adminUserPersistencePort.save(any(IdentityUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> result = adminUserService.updateRoles(USER_ID, Set.of(UserRole.SYSTEM_ADMIN));

        assertThat(result).isEqualTo(Set.of("SYSTEM_ADMIN"));
        ArgumentCaptor<IdentityUser> captor = ArgumentCaptor.forClass(IdentityUser.class);
        verify(adminUserPersistencePort).save(captor.capture());
        assertThat(captor.getValue().getRoles()).isEqualTo(Set.of(UserRole.SYSTEM_ADMIN));
    }

    @Test
    void updateRolesRejectsEmptySet() {
        IdentityUser user = newUser(Set.of(UserRole.BUYER));
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));

        var ex = assertThrows(IdentityException.class,
                () -> adminUserService.updateRoles(USER_ID, Set.of()));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.INVALID_USER_ROLE.getCode());
    }

    @Test
    void removeRolesFallsBackToBuyerWhenAllRemoved() {
        IdentityUser user = newUser(Set.of(UserRole.SYSTEM_ADMIN));
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));
        when(adminUserPersistencePort.save(any(IdentityUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> result = adminUserService.removeRoles(USER_ID, Set.of(UserRole.SYSTEM_ADMIN));

        assertThat(result).isEqualTo(Set.of("BUYER"));
    }

    @Test
    void removeRolesKeepsRemainingRoles() {
        IdentityUser user = newUser(Set.of(UserRole.BUYER, UserRole.SYSTEM_ADMIN));
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));
        when(adminUserPersistencePort.save(any(IdentityUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> result = adminUserService.removeRoles(USER_ID, Set.of(UserRole.SYSTEM_ADMIN));

        assertThat(result).isEqualTo(Set.of("BUYER"));
    }

    @Test
    void updateStatusUpdatesUserStatus() {
        IdentityUser user = newUser(Set.of(UserRole.BUYER));
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));
        when(adminUserPersistencePort.save(any(IdentityUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String status = adminUserService.updateStatus(USER_ID, UserStatus.BANNED);

        assertThat(status).isEqualTo("BANNED");
    }

    @Test
    void updateStatusRejectsNullStatus() {
        IdentityUser user = newUser(Set.of(UserRole.BUYER));
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));

        var ex = assertThrows(IdentityException.class,
                () -> adminUserService.updateStatus(USER_ID, null));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.INVALID_USER_STATUS.getCode());
    }

    @Test
    void unlockAccountUnlocksLockedUser() {
        IdentityUser user = newUser(Set.of(UserRole.BUYER));
        user.lockUntil(Instant.now().plus(Duration.ofMinutes(10)));
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));

        adminUserService.unlockAccount(USER_ID);

        ArgumentCaptor<IdentityUser> captor = ArgumentCaptor.forClass(IdentityUser.class);
        verify(adminUserPersistencePort).save(captor.capture());
        assertThat(captor.getValue().getLockedUntil()).isNull();
    }

    @Test
    void listUsersUsesOffsetPaginationAndDelegatesToPort() {
        IdentityUser user = newUser(Set.of(UserRole.BUYER));
        PageResult<IdentityUser> page = new PageResult<>(List.of(user), 0, 10, 1L);
        when(adminUserPersistencePort.findUsersWithFilters(eq(UserStatus.ACTIVE), eq(UserRole.BUYER),
                any(OffsetPagination.class))).thenReturn(page);
        UserListResult expected = new UserListResult(
                List.of(new UserListResult.UserSummary(USER_ID, "alice@example.com", "Alice", "ACTIVE", "BUYER")),
                0, 10, 1);
        when(adminResultMapper.toUserListResult(any(), eq(0), eq(10), eq(1))).thenReturn(expected);

        UserListResult result = adminUserService.listUsers(UserStatus.ACTIVE, UserRole.BUYER, 0, 10);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getUserByIdDelegatesToMapper() {
        IdentityUser user = newUser(Set.of(UserRole.BUYER));
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));
        UserDetailResult expected = new UserDetailResult(USER_ID, "alice@example.com",
                "+84912345678", "Alice", Set.of("BUYER"), "ACTIVE", null, null, null);
        when(adminResultMapper.toUserDetailResult(user)).thenReturn(expected);

        UserDetailResult result = adminUserService.getUserById(USER_ID);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getUserByIdThrowsWhenMissing() {
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.empty());

        var ex = assertThrows(IdentityException.class, () -> adminUserService.getUserById(USER_ID));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode());
    }

    private IdentityUser newUser(Set<UserRole> roles) {
        return new IdentityUser(
                USER_ID,
                "alice@example.com",
                "+84912345678",
                "alice",
                "hash",
                "Alice",
                null,
                roles,
                UserStatus.ACTIVE,
                null,
                null,
                null,
                Instant.now());
    }
}
