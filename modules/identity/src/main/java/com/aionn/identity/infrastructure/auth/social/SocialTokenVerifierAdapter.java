package com.aionn.identity.infrastructure.auth.social;

import com.aionn.identity.application.port.out.social.SocialTokenVerifierPort;
import com.aionn.identity.application.port.out.social.SocialUserProfile;
import com.aionn.identity.domain.valueobject.AuthProvider;
import com.aionn.identity.infrastructure.auth.social.google.GoogleSocialTokenVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialTokenVerifierAdapter implements SocialTokenVerifierPort {

    private final GoogleSocialTokenVerifier googleSocialTokenVerifier;

    @Override
    public SocialUserProfile verifyAndExtract(AuthProvider provider, String providerToken) {
        return switch (provider) {
            case GOOGLE -> googleSocialTokenVerifier.verify(providerToken);
        };
    }
}
