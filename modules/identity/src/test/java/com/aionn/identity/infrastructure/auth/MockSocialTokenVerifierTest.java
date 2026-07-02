package com.aionn.identity.infrastructure.auth;

import com.aionn.identity.application.port.out.social.SocialUserProfile;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.infrastructure.auth.social.google.GoogleSocialTokenVerifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MockSocialTokenVerifierTest {

    private static final GoogleSocialTokenVerifier MOCK_GOOGLE = providerToken -> {
        return new SocialUserProfile("google-uid-" + providerToken, "u@example.com", "User");
    };

    @Test
    void googleVerifierRejectsBlankProviderToken() {
        IdentityException ex = assertThrows(IdentityException.class,
                () -> MOCK_GOOGLE.requireNotBlank("   "));
        assertEquals(IdentityErrorCode.PROVIDER_TOKEN_INVALID.getCode(), ex.getErrorCode());
    }

    @Test
    void googleVerifierRejectsNullProviderToken() {
        assertThrows(IdentityException.class, () -> MOCK_GOOGLE.requireNotBlank(null));
    }

    @Test
    void requireNotBlankAcceptsRealTokens() {
        assertDoesNotThrow(() -> MOCK_GOOGLE.requireNotBlank("abc"));
    }

    @Test
    void mockVerifierReturnsSocialProfile() {
        SocialUserProfile profile = MOCK_GOOGLE.verify("token-123");
        assertEquals("google-uid-token-123", profile.providerUserId());
        assertEquals("u@example.com", profile.email());
    }
}
