package com.aionn.identity.infrastructure.auth.social.google;

import com.aionn.identity.application.port.out.social.SocialUserProfile;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleSocialTokenVerifierTest {

    private final GoogleSocialTokenVerifier verifier = token -> new SocialUserProfile("uid-" + token, "u@example.com",
            "User");

    @Test
    void requireNotBlankRejectsNull() {
        assertThatThrownBy(() -> verifier.requireNotBlank(null))
                .isInstanceOf(IdentityException.class)
                .satisfies(ex -> assertThat(((IdentityException) ex).getErrorCode())
                        .isEqualTo(IdentityErrorCode.PROVIDER_TOKEN_INVALID.getCode()));
    }

    @Test
    void requireNotBlankRejectsBlank() {
        assertThatThrownBy(() -> verifier.requireNotBlank("   "))
                .isInstanceOf(IdentityException.class);
    }

    @Test
    void requireNotBlankAcceptsNonBlank() {
        assertThatCode(() -> verifier.requireNotBlank("token")).doesNotThrowAnyException();
    }

    @Test
    void verifyReturnsProfile() {
        SocialUserProfile profile = verifier.verify("abc");

        assertThat(profile.providerUserId()).isEqualTo("uid-abc");
        assertThat(profile.email()).isEqualTo("u@example.com");
    }
}
