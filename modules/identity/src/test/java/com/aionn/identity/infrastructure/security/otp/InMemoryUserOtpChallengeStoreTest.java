package com.aionn.identity.infrastructure.security.otp;

import com.aionn.identity.application.port.out.user.UserOtpChallengeStorePort.UserOtpChallenge;
import com.aionn.identity.domain.valueobject.OtpChannel;
import com.aionn.identity.domain.valueobject.UserOtpPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryUserOtpChallengeStoreTest {

    private InMemoryUserOtpChallengeStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryUserOtpChallengeStore();
    }

    private UserOtpChallenge challenge(String userId, UserOtpPurpose purpose) {
        return new UserOtpChallenge(
                userId,
                purpose,
                OtpChannel.EMAIL,
                "target@example.com",
                "123456",
                null,
                Instant.now().plus(Duration.ofMinutes(5)),
                0);
    }

    @Test
    void savedChallengeCanBeFound() {
        UserOtpChallenge challenge = challenge("user-1", UserOtpPurpose.CHANGE_EMAIL);

        store.save(challenge);

        assertThat(store.find("user-1", UserOtpPurpose.CHANGE_EMAIL)).contains(challenge);
    }

    @Test
    void findReturnsEmptyForUnknownKey() {
        assertThat(store.find("missing", UserOtpPurpose.VERIFY_PRIMARY_EMAIL)).isEmpty();
    }

    @Test
    void keyIsScopedByUserAndPurpose() {
        UserOtpChallenge email = challenge("user-1", UserOtpPurpose.CHANGE_EMAIL);
        UserOtpChallenge phone = challenge("user-1", UserOtpPurpose.CHANGE_PHONE);

        store.save(email);
        store.save(phone);

        assertThat(store.find("user-1", UserOtpPurpose.CHANGE_EMAIL)).contains(email);
        assertThat(store.find("user-1", UserOtpPurpose.CHANGE_PHONE)).contains(phone);
    }

    @Test
    void deleteRemovesChallenge() {
        store.save(challenge("user-1", UserOtpPurpose.CHANGE_EMAIL));

        store.delete("user-1", UserOtpPurpose.CHANGE_EMAIL);

        assertThat(store.find("user-1", UserOtpPurpose.CHANGE_EMAIL)).isEmpty();
    }
}
