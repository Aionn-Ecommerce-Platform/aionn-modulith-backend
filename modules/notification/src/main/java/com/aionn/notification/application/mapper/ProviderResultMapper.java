package com.aionn.notification.application.mapper;

import com.aionn.notification.application.dto.provider.result.ProviderResult;
import com.aionn.notification.domain.model.NotificationProvider;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProviderResultMapper {

    @Mapping(target = "channel", expression = "java(provider.getChannel().name())")
    @Mapping(target = "config", expression = "java(java.util.Map.copyOf(provider.getConfig()))")
    ProviderResult toResult(NotificationProvider provider);
}
