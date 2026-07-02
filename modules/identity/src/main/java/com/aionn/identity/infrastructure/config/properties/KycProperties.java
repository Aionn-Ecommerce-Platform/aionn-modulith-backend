package com.aionn.identity.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import com.aionn.identity.domain.valueobject.KycProvider;

import java.time.Duration;

@ConfigurationProperties(prefix = "identity.kyc")
public record KycProperties(
        @DefaultValue("LOCAL") KycProvider provider,
        Sumsub sumsub,
        Local local) {

    public KycProperties {
        if (local == null) {
            local = new Local(null, null);
        }
    }

    public boolean isSumsubEnabled() {
        return provider == KycProvider.SUMSUB;
    }

    public boolean isLocalDevelopmentEnabled() {
        return provider == KycProvider.LOCAL;
    }

    public boolean usesManagedProvider() {
        return isSumsubEnabled();
    }

    public record Sumsub(
            @DefaultValue("https://api.sumsub.com") String baseUrl,
            String appToken,
            String secretKey,
            String levelName,
            String webhookSecret,
            @DefaultValue("600") int sdkTokenTtlSeconds,
            @DefaultValue("true") boolean sandbox) {
    }

    public record Local(
            String levelName,
            Duration sessionTtl) {

        private static final String DEFAULT_LEVEL_NAME = "local-dev-kyc";
        private static final Duration DEFAULT_SESSION_TTL = Duration.ofMinutes(10);

        public Local {
            if (levelName == null || levelName.isBlank()) {
                levelName = DEFAULT_LEVEL_NAME;
            }
            if (sessionTtl == null || sessionTtl.isZero() || sessionTtl.isNegative()) {
                sessionTtl = DEFAULT_SESSION_TTL;
            }
        }

        public int sessionTtlSeconds() {
            return Math.toIntExact(sessionTtl.toSeconds());
        }
    }
}
