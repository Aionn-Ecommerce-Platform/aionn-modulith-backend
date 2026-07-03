package com.aionn.identity.application.usecase.preference;

import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;
import com.aionn.identity.application.port.in.preference.GetUserPreferenceQueryPort;
import com.aionn.identity.application.service.PreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUserPreferenceUseCase implements GetUserPreferenceQueryPort {

    private final PreferenceService preferenceService;

    @Override
    @Transactional(readOnly = true)
    public UserPreferenceResult execute(String userId) {
        return preferenceService.get(userId);
    }
}

