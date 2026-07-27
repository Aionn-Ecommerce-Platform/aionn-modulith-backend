package com.aionn.notification.adapter.rest.mapper.notification;

import com.aionn.notification.adapter.rest.dto.notification.SendNotificationRequest;
import com.aionn.notification.adapter.rest.dto.notification.response.NotificationResponse;
import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.dto.notification.result.NotificationResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationDtoMapper {

    NotificationResponse toResponse(NotificationResult result);

    List<NotificationResponse> toResponses(List<NotificationResult> results);

    default NotificationCommands.SendByEvent toSendByEventCommand(SendNotificationRequest request) {
        return new NotificationCommands.SendByEvent(request.userId(), request.eventType(),
                request.category(), request.channels(), request.locale(),
                request.campaignId(), request.context());
    }

    default NotificationCommands.MarkRead toMarkReadCommand(String userId, String notiId) {
        return new NotificationCommands.MarkRead(userId, notiId);
    }

    default NotificationCommands.MarkDeleted toMarkDeletedCommand(String userId, String notiId) {
        return new NotificationCommands.MarkDeleted(userId, notiId);
    }
}
