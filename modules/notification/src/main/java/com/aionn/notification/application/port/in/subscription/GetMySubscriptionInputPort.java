package com.aionn.notification.application.port.in.subscription;

import com.aionn.notification.application.dto.subscription.result.SubscriptionResult;

public interface GetMySubscriptionInputPort {
    SubscriptionResult execute(String userId);
}