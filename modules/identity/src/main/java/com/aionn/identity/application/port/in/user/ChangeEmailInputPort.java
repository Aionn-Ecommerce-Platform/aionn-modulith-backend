package com.aionn.identity.application.port.in.user;

import com.aionn.identity.application.dto.user.view.UserProfileView;

public interface ChangeEmailInputPort {

    void sendOtp(String userId, String newEmail);

    UserProfileView confirm(String userId, String otpCode);
}

