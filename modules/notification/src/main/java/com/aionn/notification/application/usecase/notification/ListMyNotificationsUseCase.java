package com.aionn.notification.application.usecase.notification;

import com.aionn.notification.application.dto.notification.result.NotificationResult;
import com.aionn.notification.application.mapper.NotificationResultMapper;
import com.aionn.notification.application.port.in.notification.ListMyNotificationsInputPort;
import com.aionn.notification.application.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMyNotificationsUseCase implements ListMyNotificationsInputPort {

    private final NotificationDispatchService dispatchService;
    private final NotificationResultMapper notificationResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResult> execute(String userId, int limit) {
        return notificationResultMapper.toResults(dispatchService.listMine(userId, limit));
    }
}