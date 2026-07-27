package com.aionn.notification.application.usecase.notification;

import com.aionn.notification.application.dto.notification.result.NotificationResult;
import com.aionn.notification.application.mapper.NotificationResultMapper;
import com.aionn.notification.application.port.in.notification.GetMyNotificationInputPort;
import com.aionn.notification.application.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMyNotificationUseCase implements GetMyNotificationInputPort {

    private final NotificationDispatchService dispatchService;
    private final NotificationResultMapper notificationResultMapper;

    @Override
    @Transactional(readOnly = true)
    public NotificationResult execute(String userId, String notiId) {
        return notificationResultMapper.toResult(dispatchService.get(userId, notiId));
    }
}