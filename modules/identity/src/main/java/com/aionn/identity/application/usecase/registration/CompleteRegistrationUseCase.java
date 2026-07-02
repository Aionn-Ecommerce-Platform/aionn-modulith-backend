package com.aionn.identity.application.usecase.registration;

import com.aionn.identity.application.port.in.registration.CompleteRegistrationInputPort;
import com.aionn.identity.application.dto.registration.result.CompleteRegistrationResult;
import com.aionn.identity.application.dto.registration.command.CompleteRegistrationCommand;
import com.aionn.identity.application.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompleteRegistrationUseCase implements CompleteRegistrationInputPort {

    private final RegistrationService registrationService;

    @Override
    @Transactional
    public CompleteRegistrationResult execute(CompleteRegistrationCommand command) {
        return registrationService.complete(command);
    }
}



