package com.aionn.identity.infrastructure.media;

import com.aionn.identity.application.dto.media.result.UploadSignatureResult;
import com.aionn.identity.application.port.out.media.MediaUploadSignatureProviderPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "identity.media", name = "provider", havingValue = "mock")
public class MockMediaUploadSignatureProvider implements MediaUploadSignatureProviderPort {

    private final Clock clock;

    public MockMediaUploadSignatureProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public UploadSignatureResult generateAvatarUploadSignature(String userId) {
        return placeholder("identity/avatars/" + userId);
    }

    @Override
    public UploadSignatureResult generateKycDocumentUploadSignature(String userId) {
        return placeholder("identity/kyc/" + userId);
    }

    private UploadSignatureResult placeholder(String folder) {
        return new UploadSignatureResult(
                "mock-signature",
                String.valueOf(Instant.now(clock).getEpochSecond()),
                "mock-api-key",
                "mock-cloud",
                "https://example.invalid/mock-upload",
                folder);
    }
}
