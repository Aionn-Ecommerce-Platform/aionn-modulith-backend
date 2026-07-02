package com.aionn.identity.application.port.in.preference;

import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;

public interface GetUserPreferenceQueryPort {
    UserPreferenceResult execute(String userId);
}


