package com.aionn.identity.application.usecase.consent;

import com.aionn.identity.application.dto.consent.result.ConsentResult;
import com.aionn.identity.application.dto.consent.command.UpdateMarketingConsentCommand;
import com.aionn.identity.application.port.in.consent.UpdateMarketingConsentInputPort;
import com.aionn.identity.application.service.ConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateMarketingConsentUseCase implements UpdateMarketingConsentInputPort {

    private final ConsentService consentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsentResult execute(UpdateMarketingConsentCommand command) {
        return consentService.updateMarketing(command.userId(), command.subscribed(), command.clientIp());
    }
}

