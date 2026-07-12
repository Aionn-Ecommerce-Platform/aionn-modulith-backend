package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.preference.command.UpdateAiPrivacyPreferenceCommand;
import com.aionn.identity.application.dto.preference.command.UpdateGeneralPreferenceCommand;
import com.aionn.identity.application.dto.preference.command.UpdateNotificationPreferenceCommand;
import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;
import com.aionn.identity.application.mapper.UserPreferenceResultMapper;
import com.aionn.identity.application.port.out.preference.UserPreferencePersistencePort;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Mock
    private UserPersistencePort userPersistencePort;
    @Mock
    private UserPreferencePersistencePort preferencePersistencePort;

    private PreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        preferenceService = new PreferenceService(
                userPersistencePort,
                preferencePersistencePort,
                Mappers.getMapper(UserPreferenceResultMapper.class),
                Clock.systemUTC());
    }

    @Test
    void updateGeneralReplacesGeneralFieldsAndKeepsOthers() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(preferencePersistencePort.findById(USER_ID)).thenReturn(Optional.of(existing(
                "vi", "VND", "Asia/Ho_Chi_Minh", "light", "{\"push\":true}", "{}")));
        when(preferencePersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferenceResult result = preferenceService.updateGeneral(
                new UpdateGeneralPreferenceCommand(USER_ID, "en", "USD", "UTC", "dark"));

        assertThat(result.language()).isEqualTo("en");
        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.timezone()).isEqualTo("UTC");
        assertThat(result.theme()).isEqualTo("dark");
        assertThat(result.notificationSettings()).isEqualTo("{\"push\":true}");
        assertThat(result.aiPrivacySettings()).isEqualTo("{}");
    }

    @Test
    void updateNotificationsReplacesOnlyNotificationField() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(preferencePersistencePort.findById(USER_ID)).thenReturn(Optional.of(existing(
                "vi", "VND", "Asia/Ho_Chi_Minh", "light", "{\"push\":true}", "{}")));
        when(preferencePersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferenceResult result = preferenceService.updateNotifications(
                new UpdateNotificationPreferenceCommand(USER_ID, "{\"push\":false}"));

        assertThat(result.language()).isEqualTo("vi");
        assertThat(result.notificationSettings()).isEqualTo("{\"push\":false}");
        assertThat(result.aiPrivacySettings()).isEqualTo("{}");
    }

    @Test
    void updateAiPrivacyReplacesOnlyAiPrivacyField() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(preferencePersistencePort.findById(USER_ID)).thenReturn(Optional.of(existing(
                "vi", "VND", "Asia/Ho_Chi_Minh", "light", "{}", "{\"a\":1}")));
        when(preferencePersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferenceResult result = preferenceService.updateAiPrivacy(
                new UpdateAiPrivacyPreferenceCommand(USER_ID, "{\"a\":2}"));

        assertThat(result.aiPrivacySettings()).isEqualTo("{\"a\":2}");
        assertThat(result.notificationSettings()).isEqualTo("{}");
    }

    @Test
    void getCreatesDefaultWhenAbsent() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(preferencePersistencePort.findById(USER_ID)).thenReturn(Optional.empty());
        UserPreferenceResult defaultPref = existing("vi", "VND", "UTC", "light", "{}", "{}");
        when(preferencePersistencePort.createDefault(USER_ID)).thenReturn(defaultPref);

        UserPreferenceResult result = preferenceService.get(USER_ID);

        assertThat(result).isEqualTo(defaultPref);
        verify(preferencePersistencePort).createDefault(USER_ID);
    }

    @Test
    void getThrowsWhenUserMissing() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.empty());

        var ex = assertThrows(IdentityException.class, () -> preferenceService.get(USER_ID));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode());
        verify(preferencePersistencePort, never()).findById(any());
    }

    @Test
    void updateNotificationsTouchesUpdatedAt() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        UserPreferenceResult before = new UserPreferenceResult(
                USER_ID, "vi", "VND", "UTC", "light", "{}", "{}",
                Instant.now().minus(Duration.ofDays(1)));
        when(preferencePersistencePort.findById(USER_ID)).thenReturn(Optional.of(before));
        when(preferencePersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<UserPreferenceResult> captor = ArgumentCaptor.forClass(UserPreferenceResult.class);
        preferenceService.updateNotifications(new UpdateNotificationPreferenceCommand(USER_ID, "{}"));

        verify(preferencePersistencePort).save(captor.capture());
        assertThat(captor.getValue().updatedAt().isAfter(before.updatedAt())).isTrue();
    }

    private static IdentityUser activeUser() {
        return new IdentityUser(
                USER_ID,
                "alice@example.com",
                "+84912345678",
                "alice",
                "hash",
                "Alice",
                null,
                Set.of(UserRole.BUYER),
                UserStatus.ACTIVE,
                null,
                null,
                null,
                Instant.now());
    }

    private static UserPreferenceResult existing(
            String language, String currency, String timezone, String theme,
            String notification, String aiPrivacy) {
        return new UserPreferenceResult(USER_ID, language, currency, timezone, theme,
                notification, aiPrivacy, Instant.now());
    }
}
