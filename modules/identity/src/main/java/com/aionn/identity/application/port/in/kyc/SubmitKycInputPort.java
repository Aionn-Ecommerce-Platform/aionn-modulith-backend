package com.aionn.identity.application.port.in.kyc;

import com.aionn.identity.application.dto.kyc.command.SubmitKycCommand;
import com.aionn.identity.application.dto.kyc.result.KycResult;

public interface SubmitKycInputPort {
    KycResult execute(SubmitKycCommand command);
}
