package com.aionn.identity.application.port.in.preference;

import com.aionn.identity.application.dto.preference.command.UpdateGeneralPreferenceCommand;
import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;

public interface UpdateGeneralPreferenceInputPort {
    UserPreferenceResult execute(UpdateGeneralPreferenceCommand command);
}


