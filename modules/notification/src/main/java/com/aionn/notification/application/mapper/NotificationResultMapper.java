package com.aionn.notification.application.mapper;

import com.aionn.notification.application.dto.notification.result.NotificationResult;
import com.aionn.notification.domain.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationResultMapper {

    @Mapping(target = "channel", expression = "java(notification.getChannel().name())")
    @Mapping(target = "category", expression = "java(notification.getCategory().name())")
    @Mapping(target = "priority", expression = "java(notification.getPriority().name())")
    @Mapping(target = "status", expression = "java(notification.getStatus().name())")
    NotificationResult toResult(Notification notification);

    List<NotificationResult> toResults(List<Notification> notifications);
}
