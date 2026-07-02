package com.aionn.identity.application.port.in.kyc;

import com.aionn.identity.application.dto.kyc.command.KycAdminCommands;
import com.aionn.identity.application.dto.kyc.result.KycResult;

public interface MarkKycInReviewInputPort {

    KycResult execute(KycAdminCommands.MarkInReviewKyc command);
}
