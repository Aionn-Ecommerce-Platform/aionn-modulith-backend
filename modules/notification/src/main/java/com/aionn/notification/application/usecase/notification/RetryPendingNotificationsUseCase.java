package com.aionn.notification.application.usecase.notification;

import com.aionn.notification.application.port.in.notification.RetryPendingNotificationsInputPort;
import com.aionn.notification.application.service.NotificationDeliveryOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RetryPendingNotificationsUseCase implements RetryPendingNotificationsInputPort {

    private final NotificationDeliveryOrchestrator deliveryOrchestrator;

    @Override
    public int execute(int batchSize) {
        return deliveryOrchestrator.retryPending(batchSize);
    }
}