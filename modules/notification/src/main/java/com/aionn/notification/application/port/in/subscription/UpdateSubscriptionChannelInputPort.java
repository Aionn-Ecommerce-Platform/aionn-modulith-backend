package com.aionn.notification.application.port.in.subscription;

import com.aionn.notification.application.dto.subscription.command.SubscriptionCommands;
import com.aionn.notification.application.dto.subscription.result.SubscriptionResult;

public interface UpdateSubscriptionChannelInputPort {
    SubscriptionResult execute(SubscriptionCommands.UpdateChannel command);
}