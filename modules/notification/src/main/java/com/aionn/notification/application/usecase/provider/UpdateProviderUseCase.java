package com.aionn.notification.application.usecase.provider;

import com.aionn.notification.application.dto.provider.command.ProviderCommands;
import com.aionn.notification.application.dto.provider.result.ProviderResult;
import com.aionn.notification.application.mapper.ProviderResultMapper;
import com.aionn.notification.application.port.in.provider.UpdateProviderInputPort;
import com.aionn.notification.application.service.NotificationProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateProviderUseCase implements UpdateProviderInputPort {

    private final NotificationProviderService providerService;
    private final ProviderResultMapper providerResultMapper;

    @Override
    @Transactional
    public ProviderResult execute(ProviderCommands.UpdateProvider command) {
        return providerResultMapper.toResult(providerService.update(command));
    }
}