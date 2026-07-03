package com.aionn.identity.application.port.in.kyc;

import com.aionn.identity.application.dto.kyc.result.KycResult;
import com.aionn.identity.application.dto.kyc.command.CreateKycCommand;

public interface CreateKycInputPort {
    KycResult execute(CreateKycCommand command);
}
