package com.aionn.notification.application.usecase.provider;

import com.aionn.notification.application.dto.provider.command.ProviderCommands;
import com.aionn.notification.application.dto.provider.result.ProviderResult;
import com.aionn.notification.application.mapper.ProviderResultMapper;
import com.aionn.notification.application.port.in.provider.ConfigureProviderInputPort;
import com.aionn.notification.application.service.NotificationProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfigureProviderUseCase implements ConfigureProviderInputPort {

    private final NotificationProviderService providerService;
    private final ProviderResultMapper providerResultMapper;

    @Override
    @Transactional
    public ProviderResult execute(ProviderCommands.ConfigureProvider command) {
        return providerResultMapper.toResult(providerService.configure(command));
    }
}