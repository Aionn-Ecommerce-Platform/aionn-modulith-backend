package com.aionn.identity.adapter.rest.mapper.user;

import com.aionn.identity.adapter.rest.dto.user.request.ChangeAvatarRequest;
import com.aionn.identity.adapter.rest.dto.user.request.ChangeDisplayNameRequest;
import com.aionn.identity.adapter.rest.dto.user.request.RequestAccountDeletionRequest;
import com.aionn.identity.adapter.rest.dto.user.response.DataExportRequestResponse;
import com.aionn.identity.adapter.rest.dto.user.response.DeletionRequestResponse;
import com.aionn.identity.adapter.rest.dto.user.response.UserProfileResponse;
import com.aionn.identity.application.dto.user.command.CancelAccountDeletionCommand;
import com.aionn.identity.application.dto.user.command.RequestAccountDeletionCommand;
import com.aionn.identity.application.dto.user.command.ConfirmEmailChangeCommand;
import com.aionn.identity.application.dto.user.command.ConfirmEmailVerificationCommand;
import com.aionn.identity.application.dto.user.command.ConfirmPhoneChangeCommand;
import com.aionn.identity.application.dto.user.command.RequestEmailChangeOtpCommand;
import com.aionn.identity.application.dto.user.command.RequestEmailVerificationOtpCommand;
import com.aionn.identity.application.dto.user.command.RequestPhoneChangeOtpCommand;
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

    GetMyProfileQuery toGetMyProfileQuery(String userId);

    UpdateDisplayNameCommand toUpdateDisplayNameCommand(String userId, ChangeDisplayNameRequest request);

    UpdateAvatarCommand toUpdateAvatarCommand(String userId, ChangeAvatarRequest request);

    default RequestAccountDeletionCommand toRequestAccountDeletionCommand(String userId, RequestAccountDeletionRequest request) {
        return new RequestAccountDeletionCommand(
                userId,
                request != null ? request.password() : null,
                request != null ? request.mfaCode() : null);
    }

    default RequestAccountDeletionCommand toRequestAccountDeletionCommand(String userId) {
        return new RequestAccountDeletionCommand(userId, null, null);
    }

    CancelAccountDeletionCommand toCancelAccountDeletionCommand(String userId);

    RequestDataExportCommand toRequestDataExportCommand(String userId);

    UserProfileResponse toProfileResponse(UserProfileView view);

    DeletionRequestResponse toDeletionRequestResponse(DeletionRequestView view);

    DataExportRequestResponse toDataExportResponse(DataExportRequestView view);

    default RequestEmailVerificationOtpCommand toRequestEmailVerificationOtpCommand(String userId) {
        return new RequestEmailVerificationOtpCommand(userId);
    }

    default ConfirmEmailVerificationCommand toConfirmEmailVerificationCommand(String userId, String otpCode) {
        return new ConfirmEmailVerificationCommand(userId, otpCode);
    }

    default RequestEmailChangeOtpCommand toRequestEmailChangeOtpCommand(String userId, String newEmail) {
        return new RequestEmailChangeOtpCommand(userId, newEmail);
    }

    default ConfirmEmailChangeCommand toConfirmEmailChangeCommand(String userId, String otpCode) {
        return new ConfirmEmailChangeCommand(userId, otpCode);
    }

    default RequestPhoneChangeOtpCommand toRequestPhoneChangeOtpCommand(String userId, String newPhone) {
        return new RequestPhoneChangeOtpCommand(userId, newPhone);
    }

    default ConfirmPhoneChangeCommand toConfirmPhoneChangeCommand(String userId, String otpCode) {
        return new ConfirmPhoneChangeCommand(userId, otpCode);
    }
}
