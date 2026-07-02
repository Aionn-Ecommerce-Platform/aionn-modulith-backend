package com.aionn.identity.application.port.in.preference;

import com.aionn.identity.application.dto.preference.command.UpdateAiPrivacyPreferenceCommand;
import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;

public interface UpdateAiPrivacyPreferenceInputPort {
    UserPreferenceResult execute(UpdateAiPrivacyPreferenceCommand command);
}
