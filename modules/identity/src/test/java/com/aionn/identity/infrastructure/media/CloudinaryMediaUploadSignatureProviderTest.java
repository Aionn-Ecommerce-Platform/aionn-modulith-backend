package com.aionn.identity.infrastructure.media;

import java.time.Clock;

import com.aionn.identity.application.dto.media.result.UploadSignatureResult;
import com.aionn.identity.infrastructure.config.properties.CloudinaryProperties;
import com.aionn.platform.media.CloudinaryCredentialsProperties;
import com.aionn.platform.media.CloudinarySigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryMediaUploadSignatureProviderTest {

    private static final String API_SECRET = "test-api-secret";
    private static final String API_KEY = "test-api-key";
    private static final String CLOUD_NAME = "test-cloud";
    private static final String UPLOAD_BASE_URL = "https://api.cloudinary.com/v1_1";

    private CloudinaryCredentialsProperties credentials;
    private CloudinaryProperties folders;
    private CloudinaryMediaUploadSignatureProvider provider;

    @BeforeEach
    void setUp() {
        credentials = new CloudinaryCredentialsProperties(CLOUD_NAME, API_KEY, API_SECRET, UPLOAD_BASE_URL);
        folders = new CloudinaryProperties("identity/avatars", "identity/kyc");
        provider = new CloudinaryMediaUploadSignatureProvider(credentials, folders, Clock.systemUTC());
    }

    @Test
    void avatarSignatureUsesAvatarFolderAndImageResourceType() {
        UploadSignatureResult result = provider.generateAvatarUploadSignature("user-1");

        assertThat(result.folder()).isEqualTo("identity/avatars/user-1");
        assertThat(result.apiKey()).isEqualTo(API_KEY);
        assertThat(result.cloudName()).isEqualTo(CLOUD_NAME);
        assertThat(result.uploadUrl()).isEqualTo(UPLOAD_BASE_URL + "/" + CLOUD_NAME + "/image/upload");
        assertThat(result.timestamp()).isNotBlank();
        assertThat(result.type()).isEqualTo("upload");
        assertThat(result.signature()).isEqualTo(expectedSignature(result.folder(), result.timestamp(), "upload"));
    }

    @Test
    void kycSignatureUsesKycFolderAndAutoResourceType() {
        UploadSignatureResult result = provider.generateKycDocumentUploadSignature("user-2");

        assertThat(result.folder()).isEqualTo("identity/kyc/user-2");
        assertThat(result.uploadUrl()).isEqualTo(UPLOAD_BASE_URL + "/" + CLOUD_NAME + "/auto/upload");
        assertThat(result.type()).isEqualTo("private");
        assertThat(result.signature()).isEqualTo(expectedSignature(result.folder(), result.timestamp(), "private"));
    }

    @Test
    void signatureIsDeterministicForSameFolderAndTimestamp() {
        UploadSignatureResult result = provider.generateAvatarUploadSignature("user-3");

        String recomputed = expectedSignature(result.folder(), result.timestamp(), "upload");
        assertThat(result.signature()).isEqualTo(recomputed);
    }

    private String expectedSignature(String folder, String timestamp, String type) {
        Map<String, String> params = new TreeMap<>();
        params.put("folder", folder);
        params.put("timestamp", timestamp);
        params.put("type", type);
        return CloudinarySigner.sign(params, API_SECRET);
    }
}
