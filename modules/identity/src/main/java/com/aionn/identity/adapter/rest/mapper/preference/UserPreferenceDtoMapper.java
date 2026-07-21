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

@Mapper(componentModel = "spring")
public interface UserPreferenceDtoMapper {

    UpdateGeneralPreferenceCommand toUpdateGeneralCommand(String userId, GeneralPreferenceRequest request);

    UpdateNotificationPreferenceCommand toUpdateNotificationsCommand(String userId,
            NotificationPreferenceRequest request);

    UpdateAiPrivacyPreferenceCommand toUpdateAiPrivacyCommand(String userId, AiPrivacyPreferenceRequest request);

    UserPreferenceResponse toResponse(UserPreferenceResult preference);
}
