package com.aionn.promotion.infrastructure.media;

import com.aionn.promotion.application.dto.media.result.UploadSignatureResult;
import com.aionn.promotion.infrastructure.config.properties.PromotionCloudinaryProperties;
import com.aionn.sharedkernel.media.CloudinaryCredentialsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryPromotionMediaUploadSignatureProviderTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

    private CloudinaryPromotionMediaUploadSignatureProvider provider;

    @BeforeEach
    void setUp() {
        CloudinaryCredentialsProperties credentials = new CloudinaryCredentialsProperties(
                "demo-cloud", "api-key", "api-secret", "https://api.cloudinary.com/v1_1");
        PromotionCloudinaryProperties folders = new PromotionCloudinaryProperties("aionn/promotion/banners");
        provider = new CloudinaryPromotionMediaUploadSignatureProvider(credentials, folders,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void generatesBannerSignatureWithConfiguredFolder() {
        UploadSignatureResult result = provider.generateBannerUploadSignature();

        assertThat(result.folder()).isEqualTo("aionn/promotion/banners");
        assertThat(result.apiKey()).isEqualTo("api-key");
        assertThat(result.cloudName()).isEqualTo("demo-cloud");
        assertThat(result.uploadUrl()).isEqualTo("https://api.cloudinary.com/v1_1/demo-cloud/image/upload");
        assertThat(result.timestamp()).isEqualTo(String.valueOf(NOW.getEpochSecond()));
        assertThat(result.signature()).isNotBlank();
    }

    @Test
    void signatureIsStableForTheSameTimestamp() {
        String first = provider.generateBannerUploadSignature().signature();
        String second = provider.generateBannerUploadSignature().signature();

        assertThat(first).isEqualTo(second);
    }
}
