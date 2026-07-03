package com.aionn.identity.application.port.in.user;

import com.aionn.identity.application.dto.user.command.ConfirmEmailVerificationCommand;
import com.aionn.identity.application.dto.user.command.RequestEmailVerificationOtpCommand;

public interface VerifyEmailInputPort {

    void requestOtp(RequestEmailVerificationOtpCommand command);

    void confirm(ConfirmEmailVerificationCommand command);
}
