package com.aionn.identity.application.port.in.consent;

import com.aionn.identity.application.dto.consent.command.UpdateMarketingConsentCommand;
import com.aionn.identity.application.dto.consent.result.ConsentResult;

public interface UpdateMarketingConsentInputPort {
    ConsentResult execute(UpdateMarketingConsentCommand command);
}
