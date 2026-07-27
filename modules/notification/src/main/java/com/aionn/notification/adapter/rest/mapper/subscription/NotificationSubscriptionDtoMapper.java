package com.aionn.notification.adapter.rest.mapper.subscription;

import com.aionn.notification.adapter.rest.dto.subscription.RegisterDeviceTokenRequest;
import com.aionn.notification.adapter.rest.dto.subscription.UpdateSubscriptionRequest;
import com.aionn.notification.adapter.rest.dto.subscription.response.DeviceTokenResponse;
import com.aionn.notification.adapter.rest.dto.subscription.response.SubscriptionResponse;
import com.aionn.notification.application.dto.subscription.command.SubscriptionCommands;
import com.aionn.notification.application.dto.subscription.result.DeviceTokenResult;
import com.aionn.notification.application.dto.subscription.result.SubscriptionResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationSubscriptionDtoMapper {

    SubscriptionResponse toResponse(SubscriptionResult result);

    DeviceTokenResponse toResponse(DeviceTokenResult result);

    List<DeviceTokenResponse> toDeviceTokenResponses(List<DeviceTokenResult> results);

    default SubscriptionCommands.UpdateChannel toUpdateChannelCommand(String userId,
            UpdateSubscriptionRequest request) {
        return new SubscriptionCommands.UpdateChannel(userId, request.category(),
                request.channel(), request.enabled());
    }

    default SubscriptionCommands.RegisterDeviceToken toRegisterDeviceTokenCommand(String userId,
            RegisterDeviceTokenRequest request) {
        return new SubscriptionCommands.RegisterDeviceToken(userId, request.deviceToken(), request.os());
    }

    default SubscriptionCommands.RemoveDeviceToken toRemoveDeviceTokenCommand(String userId,
            String tokenId) {
        return new SubscriptionCommands.RemoveDeviceToken(userId, tokenId);
    }
}
