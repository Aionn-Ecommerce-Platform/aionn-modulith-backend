package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.admin.request.UpdateRolesRequest;
import com.aionn.identity.adapter.rest.dto.admin.request.UpdateUserStatusRequest;
import com.aionn.identity.adapter.rest.dto.admin.response.UserDetailResponse;
import com.aionn.identity.adapter.rest.dto.admin.response.UserRolesResponse;
import com.aionn.identity.adapter.rest.dto.admin.response.UserStatusResponse;
import com.aionn.identity.adapter.rest.dto.admin.response.UserSummaryResponse;
import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.adapter.rest.mapper.admin.AdminUserDtoMapper;
import com.aionn.identity.adapter.rest.mapper.security.SecurityDtoMapper;
import com.aionn.identity.application.dto.admin.command.RemoveUserRolesCommand;
import com.aionn.identity.application.dto.admin.command.UpdateUserRolesCommand;
import com.aionn.identity.application.dto.admin.command.UpdateUserStatusCommand;
import com.aionn.identity.application.dto.admin.query.GetUserQuery;
import com.aionn.identity.application.dto.admin.query.ListUsersQuery;
import com.aionn.identity.application.dto.admin.result.UserDetailResult;
import com.aionn.identity.application.dto.admin.result.UserListResult;
import com.aionn.identity.application.dto.admin.result.UserRolesResult;
import com.aionn.identity.application.dto.admin.result.UserStatusResult;
import com.aionn.identity.application.dto.security.command.UnlockAccountCommand;
import com.aionn.identity.application.port.in.admin.GetUserByIdQueryPort;
import com.aionn.identity.application.port.in.admin.GetUserAnalyticsQueryPort;
import com.aionn.identity.application.port.in.admin.ListUsersQueryPort;
import com.aionn.identity.application.port.in.admin.RemoveUserRolesInputPort;
import com.aionn.identity.application.port.in.admin.UpdateUserRolesInputPort;
import com.aionn.identity.application.port.in.admin.UpdateUserStatusInputPort;
import com.aionn.identity.application.port.in.security.UnlockAccountInputPort;
import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.sharedkernel.adapter.web.response.PageMetadata;
import com.aionn.sharedkernel.adapter.web.support.clientip.ClientIpArgumentResolver;
import com.aionn.sharedkernel.infrastructure.web.ClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerWebTest {

        @Mock
        private ListUsersQueryPort listUsersQueryPort;
        @Mock
        private GetUserByIdQueryPort getUserByIdQueryPort;
        @Mock
        private UpdateUserRolesInputPort updateUserRolesInputPort;
        @Mock
        private RemoveUserRolesInputPort removeUserRolesInputPort;
        @Mock
        private UpdateUserStatusInputPort updateUserStatusInputPort;
        @Mock
        private UnlockAccountInputPort unlockAccountInputPort;
        @Mock
        private AdminUserDtoMapper adminUserDtoMapper;
        @Mock
        private SecurityDtoMapper securityDtoMapper;
        @Mock
        private GetUserAnalyticsQueryPort getUserAnalyticsQueryPort;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                AdminUserController controller = new AdminUserController(
                                listUsersQueryPort, getUserByIdQueryPort, updateUserRolesInputPort,
                                removeUserRolesInputPort, updateUserStatusInputPort, unlockAccountInputPort,
                                getUserAnalyticsQueryPort, adminUserDtoMapper, securityDtoMapper);

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new IdentityExceptionHandler())
                                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                                                Jackson2ObjectMapperBuilder.json().build()))
                                .setCustomArgumentResolvers(new ClientIpArgumentResolver(new ClientIpResolver()))
                                .build();
        }

        @Test
        void listUsersWithoutFiltersReturnsPagedUsers() throws Exception {
                UserListResult.UserSummary u1 = new UserListResult.UserSummary(
                                "user-1", "alice@example.com", "alice_smith", "ACTIVE", "BUYER");
                UserListResult.UserSummary u2 = new UserListResult.UserSummary(
                                "user-2", "bob@example.com", "bob_jones", "ACTIVE", "MERCHANT");
                UserListResult result = new UserListResult(List.of(u1, u2), 0, 20, 2L);

                UserSummaryResponse r1 = new UserSummaryResponse(
                                "user-1", "alice@example.com", "alice_smith", UserStatus.ACTIVE, UserRole.BUYER);
                UserSummaryResponse r2 = new UserSummaryResponse(
                                "user-2", "bob@example.com", "bob_jones", UserStatus.ACTIVE, UserRole.MERCHANT);
                PageMetadata page = new PageMetadata(0, 20, 2L, 1);

                ListUsersQuery query = new ListUsersQuery(null, null, 0, 20);
                when(adminUserDtoMapper.toListUsersQuery(null, null, 0, 20)).thenReturn(query);
                when(listUsersQueryPort.execute(query)).thenReturn(result);
                when(adminUserDtoMapper.toUserSummaryResponses(result)).thenReturn(List.of(r1, r2));
                when(adminUserDtoMapper.toUserListPaging(result)).thenReturn(page);

                mockMvc.perform(get("/api/v1/admin/users"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].userId").value("user-1"))
                                .andExpect(jsonPath("$.data[1].displayName").value("bob_jones"))
                                .andExpect(jsonPath("$.paging.totalElements").value(2));

                verify(listUsersQueryPort).execute(query);
        }

        @Test
        void listUsersWithStatusAndRoleFiltersForwardsToService() throws Exception {
                UserListResult result = new UserListResult(List.of(), 0, 20, 0L);
                ListUsersQuery query = new ListUsersQuery(UserStatus.SUSPENDED, UserRole.MERCHANT, 0, 20);

                when(adminUserDtoMapper.toListUsersQuery(UserStatus.SUSPENDED, UserRole.MERCHANT, 0, 20))
                                .thenReturn(query);
                when(listUsersQueryPort.execute(query)).thenReturn(result);
                when(adminUserDtoMapper.toUserSummaryResponses(result)).thenReturn(List.of());
                when(adminUserDtoMapper.toUserListPaging(result)).thenReturn(new PageMetadata(0, 20, 0L, 0));

                mockMvc.perform(get("/api/v1/admin/users")
                                .param("status", "SUSPENDED")
                                .param("role", "MERCHANT"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.paging.totalElements").value(0));

                verify(adminUserDtoMapper).toListUsersQuery(UserStatus.SUSPENDED, UserRole.MERCHANT, 0, 20);
        }

        @Test
        void listUsersClampsOversizedPageRequestsViaSafePagination() throws Exception {
                // OffsetPagination.safe(...) clamps the requested page size; the
                // controller must always pass the safe value to the mapper, never the
                // raw user input.
                UserListResult result = new UserListResult(List.of(), 0, 100, 0L);
                ListUsersQuery query = new ListUsersQuery(null, null, 0, 100);

                when(adminUserDtoMapper.toListUsersQuery(eq(null), eq(null), eq(0), eq(100))).thenReturn(query);
                when(listUsersQueryPort.execute(query)).thenReturn(result);
                when(adminUserDtoMapper.toUserSummaryResponses(result)).thenReturn(List.of());
                when(adminUserDtoMapper.toUserListPaging(result)).thenReturn(new PageMetadata(0, 100, 0L, 0));

                mockMvc.perform(get("/api/v1/admin/users")
                                .param("page", "0")
                                .param("size", "9999"))
                                .andExpect(status().isOk());

                // Ensure the page size handed to the query is clamped, not the raw 9999.
                verify(adminUserDtoMapper).toListUsersQuery(eq(null), eq(null), eq(0), eq(100));
        }

        @Test
        void getUserByIdReturnsUserDetail() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                UserDetailResult result = new UserDetailResult(
                                "user-1", "alice@example.com", "0912345678", "alice_smith",
                                Set.of("BUYER"), "ACTIVE", now, now, now);
                UserDetailResponse response = new UserDetailResponse(
                                "user-1", "alice@example.com", "0912345678", "alice_smith",
                                Set.of(UserRole.BUYER), UserStatus.ACTIVE, now, now, now);

                when(adminUserDtoMapper.toGetUserQuery("user-1")).thenReturn(new GetUserQuery("user-1"));
                when(getUserByIdQueryPort.execute(any(GetUserQuery.class))).thenReturn(result);
                when(adminUserDtoMapper.toUserDetailResponse(result)).thenReturn(response);

                mockMvc.perform(get("/api/v1/admin/users/user-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.userId").value("user-1"))
                                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

                verify(getUserByIdQueryPort).execute(any(GetUserQuery.class));
        }

        @Test
        void updateRolesReplacesUserRoles() throws Exception {
                UserRolesResult result = new UserRolesResult(Set.of("MERCHANT", "BUYER"));
                UserRolesResponse response = new UserRolesResponse(Set.of(UserRole.MERCHANT, UserRole.BUYER));
                UpdateUserRolesCommand command = new UpdateUserRolesCommand(
                                "user-1", Set.of(UserRole.MERCHANT, UserRole.BUYER));

                when(adminUserDtoMapper.toUpdateRolesCommand(eq("user-1"), any(UpdateRolesRequest.class)))
                                .thenReturn(command);
                when(updateUserRolesInputPort.execute(command)).thenReturn(result);
                when(adminUserDtoMapper.toRolesResponse(result)).thenReturn(response);

                mockMvc.perform(put("/api/v1/admin/users/user-1/roles")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "roles": ["MERCHANT", "BUYER"]
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.roles",
                                                org.hamcrest.Matchers.containsInAnyOrder("MERCHANT", "BUYER")))
                                .andExpect(jsonPath("$.message").value("Roles updated"));

                verify(updateUserRolesInputPort).execute(command);
        }

        @Test
        void updateRolesRejectsEmptyRoleSet() throws Exception {
                mockMvc.perform(put("/api/v1/admin/users/user-1/roles")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "roles": []
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(updateUserRolesInputPort);
        }

        @Test
        void removeRolesUnassignsRolesFromUser() throws Exception {
                RemoveUserRolesCommand command = new RemoveUserRolesCommand(
                                "user-1", Set.of(UserRole.MERCHANT));
                UserRolesResult result = new UserRolesResult(Set.of("BUYER"));

                when(adminUserDtoMapper.toRemoveRolesCommand(eq("user-1"), any(UpdateRolesRequest.class)))
                                .thenReturn(command);
                when(removeUserRolesInputPort.execute(command)).thenReturn(result);

                mockMvc.perform(delete("/api/v1/admin/users/user-1/roles")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "roles": ["MERCHANT"]
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Roles removed"));

                verify(removeUserRolesInputPort).execute(command);
        }

        @Test
        void updateStatusChangesUserStatus() throws Exception {
                UserStatusResult result = new UserStatusResult("SUSPENDED");
                UserStatusResponse response = new UserStatusResponse(UserStatus.SUSPENDED);
                UpdateUserStatusCommand command = new UpdateUserStatusCommand("user-1", UserStatus.SUSPENDED);

                when(adminUserDtoMapper.toUpdateStatusCommand(eq("user-1"), any(UpdateUserStatusRequest.class)))
                                .thenReturn(command);
                when(updateUserStatusInputPort.execute(command)).thenReturn(result);
                when(adminUserDtoMapper.toStatusResponse(result)).thenReturn(response);

                mockMvc.perform(patch("/api/v1/admin/users/user-1/status")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "status": "SUSPENDED"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("SUSPENDED"))
                                .andExpect(jsonPath("$.message").value("Status updated"));

                verify(updateUserStatusInputPort).execute(command);
        }

        @Test
        void updateStatusRejectsMissingStatus() throws Exception {
                mockMvc.perform(patch("/api/v1/admin/users/user-1/status")
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(updateUserStatusInputPort);
        }

        @Test
        void unlockAccountUnlocksLockedUser() throws Exception {
                when(securityDtoMapper.toUnlockAccountCommand("user-1"))
                                .thenReturn(new UnlockAccountCommand("user-1"));
                doNothing().when(unlockAccountInputPort).execute(any(UnlockAccountCommand.class));

                mockMvc.perform(post("/api/v1/admin/users/unlock")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "userId": "user-1"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Account unlocked"));

                verify(unlockAccountInputPort).execute(any(UnlockAccountCommand.class));
        }

        @Test
        void unlockAccountRejectsBlankUserId() throws Exception {
                mockMvc.perform(post("/api/v1/admin/users/unlock")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "userId": ""
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(unlockAccountInputPort);
        }
}
