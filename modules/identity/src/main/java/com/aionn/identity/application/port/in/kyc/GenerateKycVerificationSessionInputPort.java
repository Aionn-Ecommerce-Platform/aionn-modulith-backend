package com.aionn.identity.application.port.in.kyc;

import com.aionn.identity.application.dto.kyc.command.GenerateKycVerificationSessionCommand;
import com.aionn.identity.application.dto.kyc.result.KycVerificationSessionResult;

public interface GenerateKycVerificationSessionInputPort {
    KycVerificationSessionResult execute(GenerateKycVerificationSessionCommand command);
}
