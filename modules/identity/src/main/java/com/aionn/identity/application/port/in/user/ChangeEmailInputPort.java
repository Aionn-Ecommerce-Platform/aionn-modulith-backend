package com.aionn.identity.application.port.in.user;

import com.aionn.identity.application.dto.user.command.ConfirmEmailChangeCommand;
import com.aionn.identity.application.dto.user.command.RequestEmailChangeOtpCommand;
import com.aionn.identity.application.dto.user.view.UserProfileView;

public interface ChangeEmailInputPort {

    void requestOtp(RequestEmailChangeOtpCommand command);

    UserProfileView confirm(ConfirmEmailChangeCommand command);
}
