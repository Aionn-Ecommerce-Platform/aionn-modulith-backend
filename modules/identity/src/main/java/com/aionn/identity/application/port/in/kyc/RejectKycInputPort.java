package com.aionn.identity.application.port.in.kyc;

import com.aionn.identity.application.dto.kyc.command.KycAdminCommands;
import com.aionn.identity.application.dto.kyc.result.KycResult;

public interface RejectKycInputPort {

    KycResult execute(KycAdminCommands.RejectKyc command);
}
