package com.aionn.identity.infrastructure.config.properties;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Builder
@ConfigurationProperties(prefix = "identity.registration")
public record RegistrationProperties(
                @DefaultValue("5") int maxVerifyAttempts,
                @DefaultValue("60") int resendCooldownSeconds,
                @DefaultValue("300") int otpExpirySeconds,
                @DefaultValue("30") int lockTimeoutSeconds,
                @DefaultValue("30") long sessionExpiresDays,
                @DefaultValue("+84") String defaultCountryCallingCode,
                @DefaultValue RateLimit rateLimit,
                @DefaultValue Captcha captcha,
                @DefaultValue Twilio twilio) {

        @Builder
        public record RateLimit(
                        @DefaultValue("3") int ipMaxAttempts,
                        @DefaultValue("300") int ipWindowSeconds,
                        @DefaultValue("1") int phoneMaxAttempts,
                        @DefaultValue("60") int phoneWindowSeconds) {
        }

        @Builder
        public record Captcha(
                        @DefaultValue("google") String provider,
                        @DefaultValue("") String expectedToken,
                        @DefaultValue("") String googleSiteKey,
                        @DefaultValue("") String googleSecretKey) {
        }

        @Builder
        public record Twilio(
                        @DefaultValue("false") boolean enabled,
                        @DefaultValue("") String accountSid,
                        @DefaultValue("") String authToken,
                        @DefaultValue("") String fromPhoneNumber) {
        }
}
