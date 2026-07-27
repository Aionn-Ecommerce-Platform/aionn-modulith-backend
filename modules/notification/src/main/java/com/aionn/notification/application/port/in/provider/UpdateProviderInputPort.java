package com.aionn.notification.application.port.in.provider;

import com.aionn.notification.application.dto.provider.command.ProviderCommands;
import com.aionn.notification.application.dto.provider.result.ProviderResult;

public interface UpdateProviderInputPort {
    ProviderResult execute(ProviderCommands.UpdateProvider command);
}