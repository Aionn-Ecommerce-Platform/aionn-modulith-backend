package com.aionn.identity.application.port.in.user;

import com.aionn.identity.application.dto.user.view.DataExportRequestView;
import com.aionn.identity.application.dto.user.view.DeletionRequestView;
import com.aionn.identity.application.dto.user.view.UserProfileView;

public interface UserServiceInputPort {

    UserProfileView getMyProfile(String userId);

    void sendVerifyPrimaryEmailOtp(String userId);

    void confirmVerifyPrimaryEmailOtp(String userId, String otpCode);

    UserProfileView updateDisplayName(String userId, String displayName);

    UserProfileView updateAvatar(String userId, String avatarUrl);

    void requestEmailChangeOtp(String userId, String newEmail);

    UserProfileView confirmEmailChange(String userId, String otpCode);

    void requestPhoneChangeOtp(String userId, String newPhone);

    UserProfileView confirmPhoneChange(String userId, String otpCode);

    DeletionRequestView requestAccountDeletion(String userId);

    void cancelAccountDeletion(String userId);

    DataExportRequestView requestDataExport(String userId);
}
