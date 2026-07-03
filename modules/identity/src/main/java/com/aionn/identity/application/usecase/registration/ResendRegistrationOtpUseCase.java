package com.aionn.identity.application.usecase.registration;

import com.aionn.identity.application.dto.registration.command.ResendRegistrationOtpCommand;
import com.aionn.identity.application.dto.registration.result.ResendRegistrationOtpResult;
import com.aionn.identity.application.port.in.registration.ResendRegistrationOtpInputPort;
import com.aionn.identity.application.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResendRegistrationOtpUseCase implements ResendRegistrationOtpInputPort {

    private final RegistrationService registrationService;

    @Override
    @Transactional
    public ResendRegistrationOtpResult execute(ResendRegistrationOtpCommand command) {
        return registrationService.resendOtp(command);
    }
}

