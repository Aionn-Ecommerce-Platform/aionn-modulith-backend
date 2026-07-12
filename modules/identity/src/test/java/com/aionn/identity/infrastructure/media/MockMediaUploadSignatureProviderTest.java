package com.aionn.identity.infrastructure.media;

import com.aionn.identity.application.dto.media.result.UploadSignatureResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockMediaUploadSignatureProviderTest {

    private final MockMediaUploadSignatureProvider provider = new MockMediaUploadSignatureProvider(java.time.Clock.systemUTC());

    @Test
    void avatarSignatureUsesAvatarFolderAndCannedValues() {
        UploadSignatureResult result = provider.generateAvatarUploadSignature("user-1");

        assertThat(result.folder()).isEqualTo("identity/avatars/user-1");
        assertThat(result.signature()).isEqualTo("mock-signature");
        assertThat(result.apiKey()).isEqualTo("mock-api-key");
        assertThat(result.cloudName()).isEqualTo("mock-cloud");
        assertThat(result.uploadUrl()).isEqualTo("https://example.invalid/mock-upload");
        assertThat(result.timestamp()).isNotBlank();
    }

    @Test
    void kycSignatureUsesKycFolder() {
        UploadSignatureResult result = provider.generateKycDocumentUploadSignature("user-2");

        assertThat(result.folder()).isEqualTo("identity/kyc/user-2");
        assertThat(result.signature()).isEqualTo("mock-signature");
    }
}
