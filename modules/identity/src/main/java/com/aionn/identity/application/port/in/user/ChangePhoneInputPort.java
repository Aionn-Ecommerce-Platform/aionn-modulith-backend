package com.aionn.identity.application.port.in.user;

import com.aionn.identity.application.dto.user.command.ConfirmPhoneChangeCommand;
import com.aionn.identity.application.dto.user.command.RequestPhoneChangeOtpCommand;
import com.aionn.identity.application.dto.user.view.UserProfileView;

public interface ChangePhoneInputPort {

    void requestOtp(RequestPhoneChangeOtpCommand command);

    UserProfileView confirm(ConfirmPhoneChangeCommand command);
}
