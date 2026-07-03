package com.aionn.identity.application.usecase.preference;

import com.aionn.identity.application.dto.preference.command.UpdateNotificationPreferenceCommand;
import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;
import com.aionn.identity.application.port.in.preference.UpdateNotificationPreferenceInputPort;
import com.aionn.identity.application.service.PreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateNotificationPreferenceUseCase implements UpdateNotificationPreferenceInputPort {

    private final PreferenceService preferenceService;

    @Override
    @Transactional
    public UserPreferenceResult execute(UpdateNotificationPreferenceCommand command) {
        return preferenceService.updateNotifications(command);
    }
}

