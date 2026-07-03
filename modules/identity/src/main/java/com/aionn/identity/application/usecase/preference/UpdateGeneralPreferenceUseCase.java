package com.aionn.identity.application.usecase.preference;

import com.aionn.identity.application.dto.preference.command.UpdateGeneralPreferenceCommand;
import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;
import com.aionn.identity.application.port.in.preference.UpdateGeneralPreferenceInputPort;
import com.aionn.identity.application.service.PreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateGeneralPreferenceUseCase implements UpdateGeneralPreferenceInputPort {

    private final PreferenceService preferenceService;

    @Override
    @Transactional
    public UserPreferenceResult execute(UpdateGeneralPreferenceCommand command) {
        return preferenceService.updateGeneral(command);
    }
}

