package com.aionn.notification.application.mapper;

import com.aionn.notification.application.dto.subscription.result.SubscriptionResult;
import com.aionn.notification.domain.model.NotificationSubscription;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.LinkedHashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface SubscriptionResultMapper {

    default SubscriptionResult toResult(NotificationSubscription subscription) {
        if (subscription == null) {
            return null;
        }
        return new SubscriptionResult(
                subscription.getUserId(),
                flatten(subscription),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt());
    }

    @Named("flatten")
    default Map<String, Map<String, Boolean>> flatten(NotificationSubscription subscription) {
        Map<String, Map<String, Boolean>> out = new LinkedHashMap<>();
        Map<NotificationCategory, Map<NotificationChannel, Boolean>> snapshot = subscription.snapshot();
        for (var catEntry : snapshot.entrySet()) {
            Map<String, Boolean> channels = new LinkedHashMap<>();
            for (var chEntry : catEntry.getValue().entrySet()) {
                channels.put(chEntry.getKey().name(), chEntry.getValue());
            }
            out.put(catEntry.getKey().name(), channels);
        }
        return out;
    }
}
