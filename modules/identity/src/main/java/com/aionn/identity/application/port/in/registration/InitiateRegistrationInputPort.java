package com.aionn.identity.application.port.in.registration;

import com.aionn.identity.application.dto.registration.command.InitiateRegistrationCommand;
import com.aionn.identity.application.dto.registration.result.InitiateRegistrationResult;
import com.aionn.sharedkernel.application.usecase.CommandUseCase;

public interface InitiateRegistrationInputPort
                extends CommandUseCase<InitiateRegistrationCommand, InitiateRegistrationResult> {
}

