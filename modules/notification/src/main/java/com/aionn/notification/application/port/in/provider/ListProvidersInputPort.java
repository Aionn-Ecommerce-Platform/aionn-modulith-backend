package com.aionn.notification.application.port.in.provider;

import com.aionn.notification.application.dto.provider.result.ProviderResult;

import java.util.List;

public interface ListProvidersInputPort {
    List<ProviderResult> execute();
}