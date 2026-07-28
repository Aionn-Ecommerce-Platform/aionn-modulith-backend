package com.aionn.chat.infrastructure.media;

import com.aionn.chat.application.dto.media.result.UploadSignatureResult;
import com.aionn.chat.infrastructure.config.properties.ChatCloudinaryProperties;
import com.aionn.sharedkernel.media.CloudinaryCredentialsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryChatMediaUploadSignatureProviderTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final String USER_ID = "01HZUSR0000000000000000001";

    private CloudinaryChatMediaUploadSignatureProvider provider;

    @BeforeEach
    void setUp() {
        CloudinaryCredentialsProperties credentials = new CloudinaryCredentialsProperties(
                "demo-cloud", "api-key", "api-secret", "https://api.cloudinary.com/v1_1");
        ChatCloudinaryProperties folders = new ChatCloudinaryProperties("aionn/chat/images");
        provider = new CloudinaryChatMediaUploadSignatureProvider(credentials, folders,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void scopesFolderPerUser() {
        UploadSignatureResult result = provider.generateChatImageUploadSignature(USER_ID);

        assertThat(result.folder()).isEqualTo("aionn/chat/images/" + USER_ID);
        assertThat(result.apiKey()).isEqualTo("api-key");
        assertThat(result.cloudName()).isEqualTo("demo-cloud");
        assertThat(result.uploadUrl()).isEqualTo("https://api.cloudinary.com/v1_1/demo-cloud/image/upload");
        assertThat(result.timestamp()).isEqualTo(String.valueOf(NOW.getEpochSecond()));
        assertThat(result.signature()).isNotBlank();
    }

    @Test
    void differentUsersGetDifferentSignatures() {
        String first = provider.generateChatImageUploadSignature("user-a").signature();
        String second = provider.generateChatImageUploadSignature("user-b").signature();

        assertThat(first).isNotEqualTo(second);
    }
}
