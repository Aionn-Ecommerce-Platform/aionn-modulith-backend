package com.aionn.identity.adapter.rest.mapper.preference;

import com.aionn.identity.adapter.rest.dto.preference.request.AiPrivacyPreferenceRequest;
import com.aionn.identity.adapter.rest.dto.preference.request.GeneralPreferenceRequest;
import com.aionn.identity.adapter.rest.dto.preference.request.NotificationPreferenceRequest;
import com.aionn.identity.adapter.rest.dto.preference.response.UserPreferenceResponse;
import com.aionn.identity.application.dto.preference.command.UpdateAiPrivacyPreferenceCommand;
import com.aionn.identity.application.dto.preference.command.UpdateGeneralPreferenceCommand;
import com.aionn.identity.application.dto.preference.command.UpdateNotificationPreferenceCommand;
import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserPreferenceDtoMapper {

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "language", source = "request.language")
    @Mapping(target = "currency", source = "request.currency")
    @Mapping(target = "timezone", source = "request.timezone")
    @Mapping(target = "theme", source = "request.theme")
    UpdateGeneralPreferenceCommand toUpdateGeneralCommand(String userId, GeneralPreferenceRequest request);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "notificationSettingsJson", source = "request.notificationSettingsJson")
    UpdateNotificationPreferenceCommand toUpdateNotificationsCommand(String userId,
            NotificationPreferenceRequest request);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "aiPrivacySettingsJson", source = "request.aiPrivacySettingsJson")
    UpdateAiPrivacyPreferenceCommand toUpdateAiPrivacyCommand(String userId, AiPrivacyPreferenceRequest request);

    UserPreferenceResponse toResponse(UserPreferenceResult preference);
}
