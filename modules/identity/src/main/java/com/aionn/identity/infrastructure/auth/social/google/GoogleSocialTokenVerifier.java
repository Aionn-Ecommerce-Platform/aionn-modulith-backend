package com.aionn.identity.infrastructure.auth.social.google;

import com.aionn.identity.application.port.out.social.SocialUserProfile;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;

public interface GoogleSocialTokenVerifier {

    SocialUserProfile verify(String providerToken);

    default void requireNotBlank(String providerToken) {
        if (providerToken == null || providerToken.isBlank()) {
            throw new IdentityException(IdentityErrorCode.PROVIDER_TOKEN_INVALID);
        }
    }
}
