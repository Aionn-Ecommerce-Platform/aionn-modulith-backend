package com.aionn.identity.infrastructure.config;

import com.aionn.identity.infrastructure.config.properties.JwtProperties;
import com.aionn.identity.infrastructure.config.properties.MfaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static com.aionn.identity.application.policy.IdentityValidationConstants.JWT_SECRET_MIN_LENGTH;
import static com.aionn.identity.application.policy.IdentityValidationConstants.MFA_ENCRYPTION_KEY_MIN_LENGTH;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtSecurityValidator {

    private final JwtProperties jwtProperties;
    private final MfaProperties mfaProperties;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void validateJwtSecurity() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        String secret = jwtProperties.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Missing required configuration: IDENTITY_JWT_SECRET");
        }
        String mfaKey = mfaProperties.encryptionKey();
        if (mfaKey == null || mfaKey.isBlank()) {
            throw new IllegalStateException("Missing required configuration: IDENTITY_MFA_ENCRYPTION_KEY");
        }

        if (isProd) {
            if (secret.length() < JWT_SECRET_MIN_LENGTH) {
                throw new IllegalStateException(
                        "CRITICAL: JWT secret must be at least " + JWT_SECRET_MIN_LENGTH +
                                " characters for HS256. Current length: " + secret.length());
            }
            if (MfaProperties.DEFAULT_ENCRYPTION_KEY.equals(mfaKey)) {
                throw new IllegalStateException(
                        "CRITICAL: MFA encryption key is using dev-default value in production! " +
                                "Set IDENTITY_MFA_ENCRYPTION_KEY to a secure random string.");
            }
            if (mfaKey.length() < MFA_ENCRYPTION_KEY_MIN_LENGTH) {
                throw new IllegalStateException(
                        "CRITICAL: MFA encryption key must be at least " + MFA_ENCRYPTION_KEY_MIN_LENGTH +
                                " characters. Current length: " + mfaKey.length());
            }
            log.info("JWT security validation passed for production profile");
        } else {
            if (secret.length() < JWT_SECRET_MIN_LENGTH) {
                log.warn("JWT secret is shorter than {} characters. Override via IDENTITY_JWT_SECRET.",
                        JWT_SECRET_MIN_LENGTH);
            }
            if (MfaProperties.DEFAULT_ENCRYPTION_KEY.equals(mfaKey)) {
                log.warn(
                        "MFA encryption key is using dev-default value. Override via IDENTITY_MFA_ENCRYPTION_KEY for non-dev environments.");
            } else if (mfaKey.length() < MFA_ENCRYPTION_KEY_MIN_LENGTH) {
                log.warn("MFA encryption key is shorter than {} characters. Override via IDENTITY_MFA_ENCRYPTION_KEY.",
                        MFA_ENCRYPTION_KEY_MIN_LENGTH);
            }
        }
    }
}
