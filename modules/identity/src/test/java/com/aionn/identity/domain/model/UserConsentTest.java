package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.ConsentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserConsentTest {

    @Test
    void grantedAndNotRevokedIsActive() {
        UserConsent consent = UserConsent.builder()
                .id("c-1")
                .userId("u-1")
                .consentType(ConsentType.TERMS)
                .version("v1")
                .granted(true)
                .agreedAt(LocalDateTime.now())
                .revokedAt(null)
                .ipAddress("127.0.0.1")
                .build();

        assertThat(consent.isActive()).isTrue();
        assertThat(consent.getConsentType()).isEqualTo(ConsentType.TERMS);
        assertThat(consent.getVersion()).isEqualTo("v1");
    }

    @Test
    void grantedButRevokedIsNotActive() {
        UserConsent consent = UserConsent.builder()
                .id("c-1")
                .userId("u-1")
                .consentType(ConsentType.MARKETING)
                .granted(true)
                .agreedAt(LocalDateTime.now().minusDays(2))
                .revokedAt(LocalDateTime.now())
                .build();

        assertThat(consent.isActive()).isFalse();
    }

    @Test
    void notGrantedIsNotActive() {
        UserConsent consent = UserConsent.builder()
                .id("c-1")
                .userId("u-1")
                .consentType(ConsentType.MARKETING)
                .granted(false)
                .build();

        assertThat(consent.isActive()).isFalse();
    }
}
