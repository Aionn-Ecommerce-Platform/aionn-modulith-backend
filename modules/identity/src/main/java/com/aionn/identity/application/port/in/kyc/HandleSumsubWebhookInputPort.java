package com.aionn.identity.application.port.in.kyc;

import com.aionn.identity.application.dto.kyc.command.SumsubWebhookCommand;

public interface HandleSumsubWebhookInputPort {

    void execute(SumsubWebhookCommand command);
}
