package com.aionn.notification.application.port.in.subscription;

import com.aionn.notification.application.dto.subscription.command.SubscriptionCommands;
import com.aionn.notification.application.dto.subscription.result.DeviceTokenResult;

public interface RegisterDeviceTokenInputPort {
    DeviceTokenResult execute(SubscriptionCommands.RegisterDeviceToken command);
}