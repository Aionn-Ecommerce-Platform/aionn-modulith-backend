package com.aionn.identity.application.usecase.preference;

import com.aionn.identity.application.dto.preference.command.UpdateAiPrivacyPreferenceCommand;
import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;
import com.aionn.identity.application.port.in.preference.UpdateAiPrivacyPreferenceInputPort;
import com.aionn.identity.application.service.PreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAiPrivacyPreferenceUseCase implements UpdateAiPrivacyPreferenceInputPort {

    private final PreferenceService preferenceService;

    @Override
    @Transactional
    public UserPreferenceResult execute(UpdateAiPrivacyPreferenceCommand command) {
        return preferenceService.updateAiPrivacy(command);
    }
}

