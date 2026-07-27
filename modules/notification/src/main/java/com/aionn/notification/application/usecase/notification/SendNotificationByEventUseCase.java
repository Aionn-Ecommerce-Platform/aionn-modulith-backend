package com.aionn.notification.application.usecase.notification;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.dto.notification.result.NotificationResult;
import com.aionn.notification.application.mapper.NotificationResultMapper;
import com.aionn.notification.application.port.in.notification.SendNotificationByEventInputPort;
import com.aionn.notification.application.service.NotificationDeliveryOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SendNotificationByEventUseCase implements SendNotificationByEventInputPort {

    private final NotificationDeliveryOrchestrator deliveryOrchestrator;
    private final NotificationResultMapper notificationResultMapper;

    @Override
    public List<NotificationResult> execute(NotificationCommands.SendByEvent command) {
        return notificationResultMapper.toResults(deliveryOrchestrator.sendByEvent(command));
    }
}