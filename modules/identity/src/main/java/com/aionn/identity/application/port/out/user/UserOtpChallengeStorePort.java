package com.aionn.identity.application.port.out.user;

import com.aionn.identity.domain.valueobject.OtpChannel;
import com.aionn.identity.domain.valueobject.UserOtpPurpose;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserOtpChallengeStorePort {

    void save(UserOtpChallenge challenge);

    Optional<UserOtpChallenge> find(String userId, UserOtpPurpose purpose);

    void delete(String userId, UserOtpPurpose purpose);

    record UserOtpChallenge(
            String userId,
            UserOtpPurpose purpose,
            OtpChannel channel,
            String target,
            String otpCode,
            String pendingValue,
            LocalDateTime expiresAt,
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
