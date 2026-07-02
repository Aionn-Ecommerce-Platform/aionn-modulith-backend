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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                Mappers.getMapper(UserPreferenceResultMapper.class));
    }

    @Test
    void updateGeneralReplacesGeneralFieldsAndKeepsOthers() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(preferencePersistencePort.findById(USER_ID)).thenReturn(Optional.of(existing(
                "vi", "VND", "Asia/Ho_Chi_Minh", "light", "{\"push\":true}", "{}")));
        when(preferencePersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferenceResult result = preferenceService.updateGeneral(
                new UpdateGeneralPreferenceCommand(USER_ID, "en", "USD", "UTC", "dark"));

        assertEquals("en", result.language());
        assertEquals("USD", result.currency());
        assertEquals("UTC", result.timezone());
        assertEquals("dark", result.theme());
        assertEquals("{\"push\":true}", result.notificationSettings());
        assertEquals("{}", result.aiPrivacySettings());
    }

    @Test
    void updateNotificationsReplacesOnlyNotificationField() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(preferencePersistencePort.findById(USER_ID)).thenReturn(Optional.of(existing(
                "vi", "VND", "Asia/Ho_Chi_Minh", "light", "{\"push\":true}", "{}")));
        when(preferencePersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferenceResult result = preferenceService.updateNotifications(
                new UpdateNotificationPreferenceCommand(USER_ID, "{\"push\":false}"));

        assertEquals("vi", result.language());
        assertEquals("{\"push\":false}", result.notificationSettings());
        assertEquals("{}", result.aiPrivacySettings());
    }

    @Test
    void updateAiPrivacyReplacesOnlyAiPrivacyField() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(preferencePersistencePort.findById(USER_ID)).thenReturn(Optional.of(existing(
                "vi", "VND", "Asia/Ho_Chi_Minh", "light", "{}", "{\"a\":1}")));
        when(preferencePersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferenceResult result = preferenceService.updateAiPrivacy(
                new UpdateAiPrivacyPreferenceCommand(USER_ID, "{\"a\":2}"));

        assertEquals("{\"a\":2}", result.aiPrivacySettings());
        assertEquals("{}", result.notificationSettings());
    }

    @Test
    void getCreatesDefaultWhenAbsent() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(preferencePersistencePort.findById(USER_ID)).thenReturn(Optional.empty());
        UserPreferenceResult defaultPref = existing("vi", "VND", "UTC", "light", "{}", "{}");
        when(preferencePersistencePort.createDefault(USER_ID)).thenReturn(defaultPref);

        UserPreferenceResult result = preferenceService.get(USER_ID);

        assertEquals(defaultPref, result);
        verify(preferencePersistencePort).createDefault(USER_ID);
    }

    @Test
    void getThrowsWhenUserMissing() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.empty());

        var ex = assertThrows(IdentityException.class, () -> preferenceService.get(USER_ID));

        assertEquals(IdentityErrorCode.USER_NOT_FOUND.getCode(), ex.getErrorCode());
        verify(preferencePersistencePort, never()).findById(any());
    }

    @Test
    void updateNotificationsTouchesUpdatedAt() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        UserPreferenceResult before = new UserPreferenceResult(
                USER_ID, "vi", "VND", "UTC", "light", "{}", "{}",
                LocalDateTime.now().minusDays(1));
        when(preferencePersistencePort.findById(USER_ID)).thenReturn(Optional.of(before));
        when(preferencePersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<UserPreferenceResult> captor = ArgumentCaptor.forClass(UserPreferenceResult.class);
        preferenceService.updateNotifications(new UpdateNotificationPreferenceCommand(USER_ID, "{}"));

        verify(preferencePersistencePort).save(captor.capture());
        assert captor.getValue().updatedAt().isAfter(before.updatedAt());
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
                LocalDateTime.now());
    }

    private static UserPreferenceResult existing(
            String language, String currency, String timezone, String theme,
            String notification, String aiPrivacy) {
        return new UserPreferenceResult(USER_ID, language, currency, timezone, theme,
                notification, aiPrivacy, LocalDateTime.now());
    }
}
