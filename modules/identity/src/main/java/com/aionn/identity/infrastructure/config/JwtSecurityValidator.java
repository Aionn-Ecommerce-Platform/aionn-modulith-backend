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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtSecurityValidator {

    private static final int MIN_SECRET_LENGTH = 32;
    private static final int MIN_MFA_KEY_LENGTH = 32;

    private final JwtProperties jwtProperties;
    private final MfaProperties mfaProperties;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void validateJwtSecurity() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod")
                || Arrays.asList(environment.getActiveProfiles()).contains("production");

        String secret = jwtProperties.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Missing required configuration: IDENTITY_JWT_SECRET");
        }
        String mfaKey = mfaProperties.encryptionKey();
        if (mfaKey == null || mfaKey.isBlank()) {
            throw new IllegalStateException("Missing required configuration: IDENTITY_MFA_ENCRYPTION_KEY");
        }

        if (isProd) {
            if (secret.length() < MIN_SECRET_LENGTH) {
                throw new IllegalStateException(
                        "CRITICAL: JWT secret must be at least " + MIN_SECRET_LENGTH +
                                " characters for HS256. Current length: " + secret.length());
            }
            if (MfaProperties.DEFAULT_ENCRYPTION_KEY.equals(mfaKey)) {
                throw new IllegalStateException(
                        "CRITICAL: MFA encryption key is using dev-default value in production! " +
                                "Set IDENTITY_MFA_ENCRYPTION_KEY to a secure random string.");
            }
            if (mfaKey.length() < MIN_MFA_KEY_LENGTH) {
                throw new IllegalStateException(
                        "CRITICAL: MFA encryption key must be at least " + MIN_MFA_KEY_LENGTH +
                                " characters. Current length: " + mfaKey.length());
            }
            log.info("JWT security validation passed for production profile");
        } else {
            if (secret.length() < MIN_SECRET_LENGTH) {
                log.warn("JWT secret is shorter than {} characters. Override via IDENTITY_JWT_SECRET.",
                        MIN_SECRET_LENGTH);
            }
            if (MfaProperties.DEFAULT_ENCRYPTION_KEY.equals(mfaKey)) {
                log.warn(
                        "MFA encryption key is using dev-default value. Override via IDENTITY_MFA_ENCRYPTION_KEY for non-dev environments.");
            } else if (mfaKey.length() < MIN_MFA_KEY_LENGTH) {
                log.warn("MFA encryption key is shorter than {} characters. Override via IDENTITY_MFA_ENCRYPTION_KEY.",
                        MIN_MFA_KEY_LENGTH);
            }
        }
    }
}
