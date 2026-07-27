package com.aionn.notification.application.usecase.subscription;

import com.aionn.notification.application.dto.subscription.command.SubscriptionCommands;
import com.aionn.notification.application.dto.subscription.result.DeviceTokenResult;
import com.aionn.notification.application.mapper.DeviceTokenResultMapper;
import com.aionn.notification.application.port.in.subscription.RegisterDeviceTokenInputPort;
import com.aionn.notification.application.service.NotificationSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterDeviceTokenUseCase implements RegisterDeviceTokenInputPort {

    private final NotificationSubscriptionService subscriptionService;
    private final DeviceTokenResultMapper deviceTokenResultMapper;

    @Override
    @Transactional
    public DeviceTokenResult execute(SubscriptionCommands.RegisterDeviceToken command) {
        return deviceTokenResultMapper.toResult(subscriptionService.registerDeviceToken(command));
    }
}