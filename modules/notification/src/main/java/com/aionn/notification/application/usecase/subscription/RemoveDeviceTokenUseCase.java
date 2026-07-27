package com.aionn.notification.application.usecase.subscription;

import com.aionn.notification.application.dto.subscription.command.SubscriptionCommands;
import com.aionn.notification.application.port.in.subscription.RemoveDeviceTokenInputPort;
import com.aionn.notification.application.service.NotificationSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RemoveDeviceTokenUseCase implements RemoveDeviceTokenInputPort {

    private final NotificationSubscriptionService subscriptionService;

    @Override
    @Transactional
    public void execute(SubscriptionCommands.RemoveDeviceToken command) {
        subscriptionService.removeDeviceToken(command);
    }
}