package com.aionn.identity.application.port.in.registration;

import com.aionn.identity.application.dto.registration.result.CompleteRegistrationResult;
import com.aionn.identity.application.dto.registration.command.CompleteRegistrationCommand;
import com.aionn.sharedkernel.application.usecase.CommandUseCase;

public interface CompleteRegistrationInputPort
                extends CommandUseCase<CompleteRegistrationCommand, CompleteRegistrationResult> {
}
