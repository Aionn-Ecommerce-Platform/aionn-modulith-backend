package com.aionn.identity.application.port.in.user;

import com.aionn.identity.application.dto.user.view.UserProfileView;

public interface ChangePhoneInputPort {

    void sendOtp(String userId, String newPhone);

    UserProfileView confirm(String userId, String otpCode);
}


