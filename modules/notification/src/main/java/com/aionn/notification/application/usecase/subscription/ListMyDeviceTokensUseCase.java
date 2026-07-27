package com.aionn.notification.application.usecase.subscription;

import com.aionn.notification.application.dto.subscription.result.DeviceTokenResult;
import com.aionn.notification.application.mapper.DeviceTokenResultMapper;
import com.aionn.notification.application.port.in.subscription.ListMyDeviceTokensInputPort;
import com.aionn.notification.application.service.NotificationSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMyDeviceTokensUseCase implements ListMyDeviceTokensInputPort {

    private final NotificationSubscriptionService subscriptionService;
    private final DeviceTokenResultMapper deviceTokenResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DeviceTokenResult> execute(String userId) {
        return deviceTokenResultMapper.toResults(subscriptionService.listDeviceTokens(userId));
    }
}