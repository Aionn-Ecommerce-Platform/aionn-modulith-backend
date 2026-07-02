package com.aionn.identity.application.port.in.registration;

import com.aionn.identity.application.dto.registration.command.VerifyRegistrationOtpCommand;
import com.aionn.identity.application.dto.registration.result.VerifyRegistrationOtpResult;
import com.aionn.sharedkernel.application.usecase.CommandUseCase;

public interface VerifyRegistrationOtpInputPort
        extends CommandUseCase<VerifyRegistrationOtpCommand, VerifyRegistrationOtpResult> {
}
