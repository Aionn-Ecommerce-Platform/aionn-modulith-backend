package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.user.command.UpdateAvatarCommand;
import com.aionn.identity.application.dto.user.command.UpdateDisplayNameCommand;
import com.aionn.identity.application.dto.user.query.GetMyProfileQuery;
import com.aionn.identity.application.dto.user.view.UserProfileView;
import com.aionn.identity.application.mapper.UserResultMapper;
import com.aionn.identity.application.policy.IdentityValidationConstants;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.IdentityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {

    private final UserPersistencePort userPersistencePort;
    private final UserResultMapper userResultMapper;

    @Transactional(readOnly = true)
    public UserProfileView getMyProfile(GetMyProfileQuery query) {
        log.debug("Fetching profile for user: {}", query.userId());
        return userResultMapper.toUserProfileView(getActiveUser(query.userId()));
    }

    public UserProfileView updateDisplayName(UpdateDisplayNameCommand command) {
        log.info("Updating display name for user: {}", command.userId());
        if (command.displayName() != null
                && command.displayName().length() > IdentityValidationConstants.DISPLAY_NAME_MAX_LENGTH) {
            throw new IdentityException(IdentityErrorCode.INVALID_DISPLAY_NAME,
                    "Display name exceeds " + IdentityValidationConstants.DISPLAY_NAME_MAX_LENGTH + " characters");
        }
        IdentityUser user = getActiveUser(command.userId());
        user.updateDisplayName(command.displayName());
        return userResultMapper.toUserProfileView(userPersistencePort.save(user));
    }

    public UserProfileView updateAvatar(UpdateAvatarCommand command) {
        log.info("Updating avatar for user: {}", command.userId());
        validateAvatarUrl(command.avatarUrl());
        IdentityUser user = getActiveUser(command.userId());
        user.updateAvatar(command.avatarUrl().trim());
        return userResultMapper.toUserProfileView(userPersistencePort.save(user));
    }

    private IdentityUser getActiveUser(String userId) {
        IdentityUser user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new IdentityException(IdentityErrorCode.USER_INACTIVE);
        }
        return user;
    }

    private static void validateAvatarUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new IdentityException(IdentityErrorCode.AVATAR_URL_INVALID);
        }
        if (avatarUrl.length() > IdentityValidationConstants.AVATAR_URL_MAX_LENGTH) {
            throw new IdentityException(IdentityErrorCode.AVATAR_URL_INVALID,
                    "Avatar URL exceeds maximum length");
        }
        try {
            URI uri = new URI(avatarUrl.trim());
            String scheme = uri.getScheme();
            if (scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null
                    || uri.getHost().isBlank()) {
                throw new IdentityException(IdentityErrorCode.AVATAR_URL_INVALID);
            }
        } catch (URISyntaxException ex) {
            throw new IdentityException(IdentityErrorCode.AVATAR_URL_INVALID);
        }
    }
}
