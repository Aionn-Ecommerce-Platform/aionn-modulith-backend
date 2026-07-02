package com.aionn.identity.infrastructure.config;

import com.aionn.identity.infrastructure.config.properties.KycProperties;
import com.aionn.identity.infrastructure.config.properties.RegistrationProperties;
import com.aionn.identity.infrastructure.config.properties.SocialAuthProperties;
import com.aionn.sharedkernel.media.CloudinaryCredentialsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdentityProviderConfigurationValidator implements SmartInitializingSingleton {

    @Value("${identity.media.provider:cloudinary}")
    private String mediaProvider;

    private final CloudinaryCredentialsProperties cloudinaryProperties;
    private final KycProperties kycProperties;
    private final SocialAuthProperties socialAuthProperties;
    private final RegistrationProperties registrationProperties;

    @Override
    public void afterSingletonsInstantiated() {
        validateCloudinary();
        validateSumsub();
        validateSocialProviders();
        validateCaptcha();
        validateTwilio();
        validateImplementedProviders();
    }

    private void validateCloudinary() {
        if (!"cloudinary".equalsIgnoreCase(mediaProvider)) {
            return;
        }
        requireNotBlank(cloudinaryProperties.cloudName(), "CLOUDINARY_CLOUD_NAME");
        requireNotBlank(cloudinaryProperties.apiKey(), "CLOUDINARY_API_KEY");
        requireNotBlank(cloudinaryProperties.apiSecret(), "CLOUDINARY_API_SECRET");
    }

    private void validateSumsub() {
        if (!kycProperties.isSumsubEnabled()) {
            return;
        }
        if (kycProperties.sumsub() == null) {
            throw new IllegalStateException("identity.kyc.sumsub configuration is required when KYC provider is sumsub");
        }
        requireNotBlank(kycProperties.sumsub().appToken(), "IDENTITY_KYC_SUMSUB_APP_TOKEN");
        requireNotBlank(kycProperties.sumsub().secretKey(), "IDENTITY_KYC_SUMSUB_SECRET_KEY");
        requireNotBlank(kycProperties.sumsub().levelName(), "IDENTITY_KYC_SUMSUB_LEVEL_NAME");
        requireNotBlank(kycProperties.sumsub().webhookSecret(), "IDENTITY_KYC_SUMSUB_WEBHOOK_SECRET");
    }

    private void validateSocialProviders() {
        if (socialAuthProperties.google() != null && socialAuthProperties.google().isRemote()) {
            requireNotBlank(socialAuthProperties.google().clientId(), "IDENTITY_AUTH_GOOGLE_CLIENT_ID");
        }
    }

    private void validateCaptcha() {
        if (registrationProperties.captcha() == null) {
            return;
        }
        if ("google".equalsIgnoreCase(registrationProperties.captcha().provider())) {
            requireNotBlank(registrationProperties.captcha().googleSecretKey(), "CAPTCHA_GOOGLE_SECRET_KEY");
        }
    }

    private void validateTwilio() {
        if (!registrationProperties.twilio().enabled()) {
            return;
        }
        requireNotBlank(registrationProperties.twilio().accountSid(), "TWILIO_ACCOUNT_SID");
        requireNotBlank(registrationProperties.twilio().authToken(), "TWILIO_AUTH_TOKEN");
        requireNotBlank(registrationProperties.twilio().fromPhoneNumber(), "TWILIO_FROM_PHONE_NUMBER");
    }

    private void validateImplementedProviders() {
        if ("remote".equalsIgnoreCase(mediaProvider)) {
            throw new IllegalStateException("identity.media.provider=remote is not implemented; use cloudinary");
        }
    }

    private void requireNotBlank(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration: " + envName);
        }
    }
}
