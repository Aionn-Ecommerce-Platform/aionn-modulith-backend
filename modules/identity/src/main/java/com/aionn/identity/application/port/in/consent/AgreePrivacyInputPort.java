package com.aionn.identity.application.port.in.consent;

import com.aionn.identity.application.dto.consent.command.AgreePrivacyCommand;
import com.aionn.identity.application.dto.consent.result.ConsentResult;

public interface AgreePrivacyInputPort {
    ConsentResult execute(AgreePrivacyCommand command);
}
