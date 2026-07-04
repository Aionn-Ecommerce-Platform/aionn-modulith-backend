package com.aionn.identity.adapter.rest.mapper.user;

import com.aionn.identity.adapter.rest.dto.user.request.ChangeAvatarRequest;
import com.aionn.identity.adapter.rest.dto.user.request.ChangeDisplayNameRequest;
import com.aionn.identity.adapter.rest.dto.user.response.DataExportRequestResponse;
import com.aionn.identity.adapter.rest.dto.user.response.DeletionRequestResponse;
import com.aionn.identity.adapter.rest.dto.user.response.UserProfileResponse;
import com.aionn.identity.application.dto.user.command.CancelAccountDeletionCommand;
import com.aionn.identity.application.dto.user.command.RequestAccountDeletionCommand;
import com.aionn.identity.application.dto.user.command.RequestDataExportCommand;
import com.aionn.identity.application.dto.user.command.UpdateAvatarCommand;
import com.aionn.identity.application.dto.user.command.UpdateDisplayNameCommand;
import com.aionn.identity.application.dto.user.query.GetMyProfileQuery;
import com.aionn.identity.application.dto.user.view.DataExportRequestView;
import com.aionn.identity.application.dto.user.view.DeletionRequestView;
import com.aionn.identity.application.dto.user.view.UserProfileView;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {

    // Query
    GetMyProfileQuery toGetMyProfileQuery(String userId);

    // Request -> Command
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "displayName", source = "request.displayName")
    UpdateDisplayNameCommand toUpdateDisplayNameCommand(String userId, ChangeDisplayNameRequest request);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "avatarUrl", source = "request.avatarUrl")
    UpdateAvatarCommand toUpdateAvatarCommand(String userId, ChangeAvatarRequest request);

    RequestAccountDeletionCommand toRequestAccountDeletionCommand(String userId);

    CancelAccountDeletionCommand toCancelAccountDeletionCommand(String userId);

    RequestDataExportCommand toRequestDataExportCommand(String userId);

    // View -> Response
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "emailVerifiedAt", source = "emailVerifiedAt")
    @Mapping(target = "phoneVerifiedAt", source = "phoneVerifiedAt")
    @Mapping(target = "createdAt", source = "createdAt")
    UserProfileResponse toProfileResponse(UserProfileView view);

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "requestedAt", source = "requestedAt")
    @Mapping(target = "scheduledDeletionAt", source = "scheduledDeletionAt")
    DeletionRequestResponse toDeletionRequestResponse(DeletionRequestView view);

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "requestedAt", source = "requestedAt")
    DataExportRequestResponse toDataExportResponse(DataExportRequestView view);

}
