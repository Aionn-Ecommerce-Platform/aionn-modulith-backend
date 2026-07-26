package com.aionn.notification.application.mapper;

import com.aionn.notification.application.dto.subscription.result.DeviceTokenResult;
import com.aionn.notification.domain.model.DeviceToken;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeviceTokenResultMapper {

    DeviceTokenResult toResult(DeviceToken token);
}
