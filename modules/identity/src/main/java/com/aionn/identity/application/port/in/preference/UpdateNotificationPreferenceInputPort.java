package com.aionn.identity.application.port.in.preference;

import com.aionn.identity.application.dto.preference.command.UpdateNotificationPreferenceCommand;
import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;

public interface UpdateNotificationPreferenceInputPort {
    UserPreferenceResult execute(UpdateNotificationPreferenceCommand command);
}
