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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        assertEquals(Set.of("SYSTEM_ADMIN"), result);
        ArgumentCaptor<IdentityUser> captor = ArgumentCaptor.forClass(IdentityUser.class);
        verify(adminUserPersistencePort).save(captor.capture());
        assertEquals(Set.of(UserRole.SYSTEM_ADMIN), captor.getValue().getRoles());
    }

    @Test
    void updateRolesRejectsEmptySet() {
        IdentityUser user = newUser(Set.of(UserRole.BUYER));
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));

        var ex = assertThrows(IdentityException.class,
                () -> adminUserService.updateRoles(USER_ID, Set.of()));

        assertEquals(IdentityErrorCode.INVALID_USER_ROLE.getCode(), ex.getErrorCode());
    }

    @Test
    void removeRolesFallsBackToBuyerWhenAllRemoved() {
        IdentityUser user = newUser(Set.of(UserRole.SYSTEM_ADMIN));
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));
        when(adminUserPersistencePort.save(any(IdentityUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> result = adminUserService.removeRoles(USER_ID, Set.of(UserRole.SYSTEM_ADMIN));

        assertEquals(Set.of("BUYER"), result);
    }

    @Test
    void removeRolesKeepsRemainingRoles() {
        IdentityUser user = newUser(Set.of(UserRole.BUYER, UserRole.SYSTEM_ADMIN));
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));
        when(adminUserPersistencePort.save(any(IdentityUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> result = adminUserService.removeRoles(USER_ID, Set.of(UserRole.SYSTEM_ADMIN));

        assertEquals(Set.of("BUYER"), result);
    }

    @Test
    void updateStatusUpdatesUserStatus() {
        IdentityUser user = newUser(Set.of(UserRole.BUYER));
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));
        when(adminUserPersistencePort.save(any(IdentityUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String status = adminUserService.updateStatus(USER_ID, UserStatus.BANNED);

        assertEquals("BANNED", status);
    }

    @Test
    void updateStatusRejectsNullStatus() {
        IdentityUser user = newUser(Set.of(UserRole.BUYER));
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));

        var ex = assertThrows(IdentityException.class,
                () -> adminUserService.updateStatus(USER_ID, null));

        assertEquals(IdentityErrorCode.INVALID_USER_STATUS.getCode(), ex.getErrorCode());
    }

    @Test
    void unlockAccountUnlocksLockedUser() {
        IdentityUser user = newUser(Set.of(UserRole.BUYER));
        user.lockUntil(LocalDateTime.now().plusMinutes(10));
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));

        adminUserService.unlockAccount(USER_ID);

        ArgumentCaptor<IdentityUser> captor = ArgumentCaptor.forClass(IdentityUser.class);
        verify(adminUserPersistencePort).save(captor.capture());
        assertNull(captor.getValue().getLockedUntil());
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

        assertEquals(expected, result);
    }

    @Test
    void getUserByIdDelegatesToMapper() {
        IdentityUser user = newUser(Set.of(UserRole.BUYER));
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));
        UserDetailResult expected = new UserDetailResult(USER_ID, "alice@example.com",
                "+84912345678", "Alice", Set.of("BUYER"), "ACTIVE", null, null, null);
        when(adminResultMapper.toUserDetailResult(user)).thenReturn(expected);

        UserDetailResult result = adminUserService.getUserById(USER_ID);

        assertEquals(expected, result);
    }

    @Test
    void getUserByIdThrowsWhenMissing() {
        when(adminUserPersistencePort.findById(USER_ID)).thenReturn(Optional.empty());

        var ex = assertThrows(IdentityException.class, () -> adminUserService.getUserById(USER_ID));

        assertEquals(IdentityErrorCode.USER_NOT_FOUND.getCode(), ex.getErrorCode());
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
                LocalDateTime.now());
    }
}
