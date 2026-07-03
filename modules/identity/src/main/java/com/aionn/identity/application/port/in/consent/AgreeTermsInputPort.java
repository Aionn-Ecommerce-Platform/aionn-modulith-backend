package com.aionn.identity.application.port.in.consent;

import com.aionn.identity.application.dto.consent.command.AgreeTermsCommand;
import com.aionn.identity.application.dto.consent.result.ConsentResult;

public interface AgreeTermsInputPort {
    ConsentResult execute(AgreeTermsCommand command);
}
