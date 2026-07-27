package com.aionn.notification.adapter.rest.mapper.provider;

import com.aionn.notification.adapter.rest.dto.provider.ConfigureProviderRequest;
import com.aionn.notification.adapter.rest.dto.provider.UpdateProviderRequest;
import com.aionn.notification.adapter.rest.dto.provider.response.ProviderResponse;
import com.aionn.notification.application.dto.provider.command.ProviderCommands;
import com.aionn.notification.application.dto.provider.result.ProviderResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationProviderDtoMapper {

    ProviderResponse toResponse(ProviderResult result);

    List<ProviderResponse> toResponses(List<ProviderResult> results);

    default ProviderCommands.ConfigureProvider toConfigureCommand(String adminId,
            ConfigureProviderRequest request) {
        return new ProviderCommands.ConfigureProvider(request.channel(), request.providerType(),
                request.config(), request.rateLimitPerMinute(), adminId);
    }

    default ProviderCommands.UpdateProvider toUpdateCommand(String providerId, String adminId,
            UpdateProviderRequest request) {
        return new ProviderCommands.UpdateProvider(providerId, request.config(),
                request.rateLimitPerMinute(), request.active(), adminId);
    }
}
