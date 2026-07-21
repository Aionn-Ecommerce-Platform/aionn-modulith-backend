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

@Mapper(componentModel = "spring")
public interface UserDtoMapper {

    // Query
    GetMyProfileQuery toGetMyProfileQuery(String userId);

    // Request -> Command
    UpdateDisplayNameCommand toUpdateDisplayNameCommand(String userId, ChangeDisplayNameRequest request);

    UpdateAvatarCommand toUpdateAvatarCommand(String userId, ChangeAvatarRequest request);

    RequestAccountDeletionCommand toRequestAccountDeletionCommand(String userId);

    CancelAccountDeletionCommand toCancelAccountDeletionCommand(String userId);

    RequestDataExportCommand toRequestDataExportCommand(String userId);

    // View -> Response
    UserProfileResponse toProfileResponse(UserProfileView view);

    DeletionRequestResponse toDeletionRequestResponse(DeletionRequestView view);

    DataExportRequestResponse toDataExportResponse(DataExportRequestView view);

}
