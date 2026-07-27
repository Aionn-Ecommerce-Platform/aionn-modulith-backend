package com.aionn.notification.application.usecase.subscription;

import com.aionn.notification.application.dto.subscription.result.SubscriptionResult;
import com.aionn.notification.application.mapper.SubscriptionResultMapper;
import com.aionn.notification.application.port.in.subscription.GetMySubscriptionInputPort;
import com.aionn.notification.application.service.NotificationSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMySubscriptionUseCase implements GetMySubscriptionInputPort {

    private final NotificationSubscriptionService subscriptionService;
    private final SubscriptionResultMapper subscriptionResultMapper;

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResult execute(String userId) {
        return subscriptionResultMapper.toResult(subscriptionService.get(userId));
    }
}