package com.aionn.identity.application.port.out.user;

import com.aionn.identity.domain.valueobject.OtpChannel;
import com.aionn.identity.domain.valueobject.UserOtpPurpose;

import java.time.Instant;
import java.util.Optional;

public interface UserOtpChallengeStorePort {

    void save(UserOtpChallenge challenge);

    Optional<UserOtpChallenge> find(String userId, UserOtpPurpose purpose);

    /**
     * Atomically reserves one verification attempt for the challenge. Returns {@code false}
     * once {@code maxAttempts} have been used, so parallel guesses cannot bypass the limit.
     */
    boolean tryAcquireAttempt(String userId, UserOtpPurpose purpose, int maxAttempts,
            Instant expiresAt);

    void resetAttempts(String userId, UserOtpPurpose purpose);

    void delete(String userId, UserOtpPurpose purpose);

    record UserOtpChallenge(
            String userId,
            UserOtpPurpose purpose,
            OtpChannel channel,
            String target,
            String otpCode,
            String pendingValue,
            Instant expiresAt,
            int attempts) {

        public UserOtpChallenge withAttempts(int nextAttempts) {
            return new UserOtpChallenge(
                    userId,
                    purpose,
                    channel,
                    target,
                    otpCode,
                    pendingValue,
                    expiresAt,
                    nextAttempts);
        }
    }
}
