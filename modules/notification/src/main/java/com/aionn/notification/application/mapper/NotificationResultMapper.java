package com.aionn.notification.application.mapper;

import com.aionn.notification.application.dto.notification.result.NotificationResult;
import com.aionn.notification.application.dto.provider.result.ProviderResult;
import com.aionn.notification.application.dto.subscription.result.DeviceTokenResult;
import com.aionn.notification.application.dto.subscription.result.SubscriptionResult;
import com.aionn.notification.application.dto.template.result.TemplateResult;
import com.aionn.notification.domain.model.DeviceToken;
import com.aionn.notification.domain.model.Notification;
import com.aionn.notification.domain.model.NotificationProvider;
import com.aionn.notification.domain.model.NotificationSubscription;
import com.aionn.notification.domain.model.NotificationTemplate;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.LinkedHashMap;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationResultMapper {

    NotificationResult toResult(Notification n);

    TemplateResult toResult(NotificationTemplate t);

    DeviceTokenResult toResult(DeviceToken t);

    ProviderResult toResult(NotificationProvider p);

    @Mapping(target = "settings", expression = "java(toSettingsSnapshot(s.snapshot()))")
    SubscriptionResult toResult(NotificationSubscription s);

    default Map<String, Map<String, Boolean>> toSettingsSnapshot(
            Map<NotificationCategory, Map<NotificationChannel, Boolean>> source) {
        Map<String, Map<String, Boolean>> out = new LinkedHashMap<>();
        source.forEach((cat, byChannel) -> {
            Map<String, Boolean> sub = new LinkedHashMap<>();
            byChannel.forEach((ch, enabled) -> sub.put(ch.name(), enabled));
            out.put(cat.name(), sub);
        });
        return out;
    }
}
