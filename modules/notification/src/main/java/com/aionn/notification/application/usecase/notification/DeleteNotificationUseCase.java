package com.aionn.notification.application.usecase.notification;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.dto.notification.result.NotificationResult;
import com.aionn.notification.application.mapper.NotificationResultMapper;
import com.aionn.notification.application.port.in.notification.DeleteNotificationInputPort;
import com.aionn.notification.application.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteNotificationUseCase implements DeleteNotificationInputPort {

    private final NotificationDispatchService dispatchService;
    private final NotificationResultMapper notificationResultMapper;

    @Override
    @Transactional
    public NotificationResult execute(NotificationCommands.MarkDeleted command) {
        return notificationResultMapper.toResult(dispatchService.delete(command));
    }
}