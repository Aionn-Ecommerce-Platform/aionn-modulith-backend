package com.aionn.identity.application.port.in.registration;

import com.aionn.identity.application.dto.registration.command.ResendRegistrationOtpCommand;
import com.aionn.identity.application.dto.registration.result.ResendRegistrationOtpResult;
import com.aionn.sharedkernel.application.usecase.CommandUseCase;

public interface ResendRegistrationOtpInputPort
        extends CommandUseCase<ResendRegistrationOtpCommand, ResendRegistrationOtpResult> {
}
