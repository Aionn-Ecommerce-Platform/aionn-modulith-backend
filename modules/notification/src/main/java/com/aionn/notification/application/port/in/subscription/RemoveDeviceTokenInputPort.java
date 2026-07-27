package com.aionn.notification.application.port.in.subscription;

import com.aionn.notification.application.dto.subscription.command.SubscriptionCommands;

public interface RemoveDeviceTokenInputPort {
    void execute(SubscriptionCommands.RemoveDeviceToken command);
}