package com.aionn.identity.infrastructure.media;

import com.aionn.identity.application.dto.media.result.UploadSignatureResult;
import com.aionn.identity.application.port.out.media.MediaUploadSignatureProviderPort;
import com.aionn.identity.infrastructure.config.properties.CloudinaryProperties;
import com.aionn.sharedkernel.media.CloudinaryCredentialsProperties;
import com.aionn.sharedkernel.media.CloudinarySigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "identity.media", name = "provider", havingValue = "cloudinary")
public class CloudinaryMediaUploadSignatureProvider implements MediaUploadSignatureProviderPort {

    private final CloudinaryCredentialsProperties credentials;
    private final CloudinaryProperties folders;

    @Override
    public UploadSignatureResult generateAvatarUploadSignature(String userId) {
        return sign(folders.avatarFolder() + "/" + userId, "image");
    }

    @Override
    public UploadSignatureResult generateKycDocumentUploadSignature(String userId) {
        return sign(folders.kycFolder() + "/" + userId, "auto");
    }

    private UploadSignatureResult sign(String folder, String resourceType) {
        long timestamp = Instant.now().getEpochSecond();
        Map<String, String> params = new TreeMap<>();
        params.put("folder", folder);
        params.put("timestamp", String.valueOf(timestamp));
        String signature = CloudinarySigner.sign(params, credentials.apiSecret());
        // Don't log the raw folder; it embeds the user id.
        log.debug("Generated {} upload signature", resourceType);
        return new UploadSignatureResult(
                signature,
                String.valueOf(timestamp),
                credentials.apiKey(),
                credentials.cloudName(),
                credentials.uploadUrl(resourceType),
                folder);
    }
}
