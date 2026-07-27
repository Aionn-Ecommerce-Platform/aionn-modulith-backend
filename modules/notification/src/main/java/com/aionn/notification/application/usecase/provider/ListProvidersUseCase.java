package com.aionn.notification.application.usecase.provider;

import com.aionn.notification.application.dto.provider.result.ProviderResult;
import com.aionn.notification.application.mapper.ProviderResultMapper;
import com.aionn.notification.application.port.in.provider.ListProvidersInputPort;
import com.aionn.notification.application.service.NotificationProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListProvidersUseCase implements ListProvidersInputPort {

    private final NotificationProviderService providerService;
    private final ProviderResultMapper providerResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProviderResult> execute() {
        return providerResultMapper.toResults(providerService.listAll());
    }
}