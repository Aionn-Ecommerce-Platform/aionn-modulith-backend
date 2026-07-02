package com.aionn.identity.adapter.rest.mapper.admin;

import com.aionn.identity.adapter.rest.dto.admin.request.UpdateRolesRequest;
import com.aionn.identity.adapter.rest.dto.admin.request.UpdateUserStatusRequest;
import com.aionn.identity.adapter.rest.dto.admin.response.UserDetailResponse;
import com.aionn.identity.adapter.rest.dto.admin.response.UserRolesResponse;
import com.aionn.identity.adapter.rest.dto.admin.response.UserStatusResponse;
import com.aionn.identity.adapter.rest.dto.admin.response.UserSummaryResponse;
import com.aionn.identity.application.dto.admin.query.GetUserQuery;
import com.aionn.identity.application.dto.admin.command.RemoveUserRolesCommand;
import com.aionn.identity.application.dto.admin.result.UserDetailResult;
import com.aionn.identity.application.dto.admin.command.UpdateUserRolesCommand;
import com.aionn.identity.application.dto.admin.command.UpdateUserStatusCommand;
import com.aionn.identity.application.dto.admin.query.ListUsersQuery;
import com.aionn.identity.application.dto.admin.result.UserListResult;
import com.aionn.identity.application.dto.admin.result.UserRolesResult;
import com.aionn.identity.application.dto.admin.result.UserStatusResult;
import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.sharedkernel.adapter.web.response.PageMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminUserDtoMapper {

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "roles", source = "request.roles")
    UpdateUserRolesCommand toUpdateRolesCommand(String userId, UpdateRolesRequest request);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "roles", source = "request.roles")
    RemoveUserRolesCommand toRemoveRolesCommand(String userId, UpdateRolesRequest request);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "status", source = "request.status")
    UpdateUserStatusCommand toUpdateStatusCommand(String userId, UpdateUserStatusRequest request);

    ListUsersQuery toListUsersQuery(UserStatus status, UserRole role, int page, int size);

    default GetUserQuery toGetUserQuery(String userId) {
        return new GetUserQuery(userId);
    }

    UserRolesResponse toRolesResponse(UserRolesResult result);

    UserStatusResponse toStatusResponse(UserStatusResult result);

    UserSummaryResponse toUserSummaryResponse(UserListResult.UserSummary user);

    UserDetailResponse toUserDetailResponse(UserDetailResult result);

    default List<UserSummaryResponse> toUserSummaryResponses(UserListResult result) {
        return result.users().stream()
                .map(this::toUserSummaryResponse)
                .toList();
    }

    default PageMetadata toUserListPaging(UserListResult result) {
        int totalPages = result.size() > 0
                ? (int) Math.ceil((double) result.total() / result.size())
                : 0;
        return new PageMetadata(
                result.page(),
                result.size(),
                result.total(),
                totalPages);
    }
}
