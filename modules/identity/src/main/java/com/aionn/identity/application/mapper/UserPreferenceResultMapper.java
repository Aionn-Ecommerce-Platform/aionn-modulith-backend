package com.aionn.identity.application.mapper;

import com.aionn.identity.application.dto.preference.command.UpdateAiPrivacyPreferenceCommand;
import com.aionn.identity.application.dto.preference.command.UpdateGeneralPreferenceCommand;
import com.aionn.identity.application.dto.preference.command.UpdateNotificationPreferenceCommand;
import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;
import org.mapstruct.Mapper;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface UserPreferenceResultMapper {

    default UserPreferenceResult applyGeneral(
            UserPreferenceResult current,
            UpdateGeneralPreferenceCommand command,
            Instant updatedAt) {
        return new UserPreferenceResult(
                current.userId(),
                command.language(),
                command.currency(),
                command.timezone(),
                command.theme(),
                current.notificationSettings(),
                current.aiPrivacySettings(),
                updatedAt);
    }

    default UserPreferenceResult applyNotifications(
            UserPreferenceResult current,
            UpdateNotificationPreferenceCommand command,
            Instant updatedAt) {
        return new UserPreferenceResult(
                current.userId(),
                current.language(),
                current.currency(),
                current.timezone(),
                current.theme(),
                command.notificationSettingsJson(),
                current.aiPrivacySettings(),
                updatedAt);
    }

    default UserPreferenceResult applyAiPrivacy(
            UserPreferenceResult current,
            UpdateAiPrivacyPreferenceCommand command,
            Instant updatedAt) {
        return new UserPreferenceResult(
                current.userId(),
                current.language(),
                current.currency(),
                current.timezone(),
                current.theme(),
                current.notificationSettings(),
                command.aiPrivacySettingsJson(),
                updatedAt);
    }
}
