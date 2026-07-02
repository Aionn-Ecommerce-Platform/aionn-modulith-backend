package com.aionn.identity.infrastructure.auth;

import com.aionn.identity.application.port.out.social.SocialUserProfile;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.valueobject.AuthProvider;
import com.aionn.identity.infrastructure.auth.social.SocialTokenVerifierAdapter;
import com.aionn.identity.infrastructure.auth.social.google.GoogleSocialTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialTokenVerifierAdapterTest {

    @Mock
    private GoogleSocialTokenVerifier googleVerifier;

    private SocialTokenVerifierAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SocialTokenVerifierAdapter(googleVerifier);
    }

    @Test
    void googleProviderDelegatesToGoogleVerifier() {
        SocialUserProfile expected = new SocialUserProfile("g-1", "u@example.com", "User");
        when(googleVerifier.verify("token-g")).thenReturn(expected);

        SocialUserProfile actual = adapter.verifyAndExtract(AuthProvider.GOOGLE, "token-g");

        assertSame(expected, actual);
        verify(googleVerifier).verify("token-g");
    }

    @Test
    void googleVerifierFailureBubblesUp() {
        when(googleVerifier.verify("bad-token")).thenThrow(
                new IdentityException(IdentityErrorCode.PROVIDER_TOKEN_INVALID));

        IdentityException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IdentityException.class,
                () -> adapter.verifyAndExtract(AuthProvider.GOOGLE, "bad-token"));

        assertEquals(IdentityErrorCode.PROVIDER_TOKEN_INVALID.getCode(), ex.getErrorCode());
    }
}
