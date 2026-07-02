package com.aionn.identity.infrastructure.auth.social;

import com.aionn.identity.application.port.out.social.SocialUserProfile;
import com.aionn.identity.domain.valueobject.AuthProvider;
import com.aionn.identity.infrastructure.auth.social.google.GoogleSocialTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        SocialUserProfile profile = new SocialUserProfile("google-user-1", "u@example.com", "Test User");
        when(googleVerifier.verify("token")).thenReturn(profile);

        SocialUserProfile result = adapter.verifyAndExtract(AuthProvider.GOOGLE, "token");

        assertEquals("google-user-1", result.providerUserId());
        verify(googleVerifier).verify("token");
    }
}
