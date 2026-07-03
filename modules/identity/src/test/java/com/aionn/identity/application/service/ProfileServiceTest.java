package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.user.command.UpdateAvatarCommand;
import com.aionn.identity.application.dto.user.command.UpdateDisplayNameCommand;
import com.aionn.identity.application.dto.user.query.GetMyProfileQuery;
import com.aionn.identity.application.dto.user.view.UserProfileView;
import com.aionn.identity.application.mapper.UserResultMapper;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ProfileServiceTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Mock
    private UserPersistencePort userPersistencePort;
    @Mock
    private UserResultMapper userResultMapper;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(userPersistencePort, userResultMapper);
    }

    @Test
    void getMyProfileReturnsActiveUserProfile() {
        IdentityUser user = activeUser();
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));
        UserProfileView expected = profileView(user);
        when(userResultMapper.toUserProfileView(user)).thenReturn(expected);

        UserProfileView result = profileService.getMyProfile(new GetMyProfileQuery(USER_ID));

        assertEquals(expected, result);
    }

    @Test
    void getMyProfileRejectsInactiveUser() {
        IdentityUser user = bannedUser();
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));

        var ex = assertThrows(IdentityException.class,
                () -> profileService.getMyProfile(new GetMyProfileQuery(USER_ID)));

        assertEquals(IdentityErrorCode.USER_INACTIVE.getCode(), ex.getErrorCode());
    }

    @Test
    void updateDisplayNameSavesTrimmedDisplayName() {
        IdentityUser user = activeUser();
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userPersistencePort.save(any(IdentityUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userResultMapper.toUserProfileView(any(IdentityUser.class)))
                .thenAnswer(invocation -> profileView(invocation.getArgument(0)));

        profileService.updateDisplayName(new UpdateDisplayNameCommand(USER_ID, "  Bob  "));

        assertEquals("Bob", user.getDisplayName());
    }

    @Test
    void updateDisplayNameRejectsTooLong() {
        String longName = "x".repeat(101);

        var ex = assertThrows(IdentityException.class,
                () -> profileService.updateDisplayName(new UpdateDisplayNameCommand(USER_ID, longName)));

        assertEquals(IdentityErrorCode.INVALID_DISPLAY_NAME.getCode(), ex.getErrorCode());
        verify(userPersistencePort, never()).save(any());
    }

    @Test
    void updateAvatarAcceptsHttpsUrl() {
        IdentityUser user = activeUser();
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userPersistencePort.save(any(IdentityUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userResultMapper.toUserProfileView(any(IdentityUser.class)))
                .thenAnswer(invocation -> profileView(invocation.getArgument(0)));

        profileService.updateAvatar(new UpdateAvatarCommand(USER_ID, "https://cdn.example/avatar.png"));

        assertEquals("https://cdn.example/avatar.png", user.getAvatarUrl());
    }

    @Test
    void updateAvatarRejectsInvalidScheme() {
        var ex = assertThrows(IdentityException.class,
                () -> profileService.updateAvatar(new UpdateAvatarCommand(USER_ID, "ftp://x.com/a.png")));

        assertEquals(IdentityErrorCode.AVATAR_URL_INVALID.getCode(), ex.getErrorCode());
    }

    @Test
    void updateAvatarRejectsBlankUrl() {
        var ex = assertThrows(IdentityException.class,
                () -> profileService.updateAvatar(new UpdateAvatarCommand(USER_ID, "  ")));

        assertEquals(IdentityErrorCode.AVATAR_URL_INVALID.getCode(), ex.getErrorCode());
    }

    @Test
    void updateAvatarRejectsTooLongUrl() {
        String longUrl = "https://x.com/" + "a".repeat(2050);

        var ex = assertThrows(IdentityException.class,
                () -> profileService.updateAvatar(new UpdateAvatarCommand(USER_ID, longUrl)));

        assertEquals(IdentityErrorCode.AVATAR_URL_INVALID.getCode(), ex.getErrorCode());
    }

    private static IdentityUser activeUser() {
        return new IdentityUser(
                USER_ID, "alice@example.com", "+84912345678", "alice",
                "hash", "Alice", null,
                Set.of(UserRole.BUYER), UserStatus.ACTIVE,
                null, null, null, LocalDateTime.now());
    }

    private static IdentityUser bannedUser() {
        return new IdentityUser(
                USER_ID, "alice@example.com", "+84912345678", "alice",
                "hash", "Alice", null,
                Set.of(UserRole.BUYER), UserStatus.BANNED,
                null, null, null, LocalDateTime.now());
    }

    private static UserProfileView profileView(IdentityUser user) {
        return new UserProfileView(
                user.getUserId(), user.getEmail(), user.getPhone(), user.getUsername(),
                user.getDisplayName(), user.getAvatarUrl(),
                user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()),
                user.getStatus().name(),
                user.getEmailVerifiedAt(), user.getPhoneVerifiedAt(), user.getCreatedAt());
    }
}
