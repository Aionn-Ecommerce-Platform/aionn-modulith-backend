package com.aionn.notification.application.usecase.notification;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.dto.notification.result.NotificationResult;
import com.aionn.notification.application.mapper.NotificationResultMapper;
import com.aionn.notification.application.port.in.notification.SendDirectNotificationInputPort;
import com.aionn.notification.application.service.NotificationDeliveryOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendDirectNotificationUseCase implements SendDirectNotificationInputPort {

    private final NotificationDeliveryOrchestrator deliveryOrchestrator;
    private final NotificationResultMapper notificationResultMapper;

    @Override
    public NotificationResult execute(NotificationCommands.SendDirectByEvent command) {
        return notificationResultMapper.toResult(deliveryOrchestrator.sendDirectByEvent(command));
    }
}