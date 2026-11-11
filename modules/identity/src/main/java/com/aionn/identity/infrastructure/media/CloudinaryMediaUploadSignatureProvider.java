package com.aionn.identity.infrastructure.media;

import com.aionn.identity.application.dto.media.result.UploadSignatureResult;
import com.aionn.identity.application.port.out.media.MediaUploadSignatureProviderPort;
import com.aionn.identity.infrastructure.config.properties.CloudinaryProperties;
import com.aionn.platform.media.CloudinaryCredentialsProperties;
import com.aionn.platform.media.CloudinarySigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "identity.media", name = "provider", havingValue = "cloudinary", matchIfMissing = true)
public class CloudinaryMediaUploadSignatureProvider implements MediaUploadSignatureProviderPort {

    private final CloudinaryCredentialsProperties credentials;
    private final CloudinaryProperties folders;
    private final Clock clock;

    @Override
    public UploadSignatureResult generateAvatarUploadSignature(String userId) {
        return sign(folders.avatarFolder() + "/" + userId, "image", "upload");
    }

    @Override
    public UploadSignatureResult generateKycDocumentUploadSignature(String userId) {
        return sign(folders.kycFolder() + "/" + userId, "auto", "private");
    }

    private UploadSignatureResult sign(String folder, String resourceType, String type) {
        long timestamp = clock.instant().getEpochSecond();
        Map<String, String> params = new TreeMap<>();
        params.put("folder", folder);
        params.put("timestamp", String.valueOf(timestamp));
        params.put("type", type);
        String signature = CloudinarySigner.sign(params, credentials.apiSecret());
        log.debug("Generated {} upload signature", resourceType);
        return new UploadSignatureResult(
                signature,
                String.valueOf(timestamp),
                credentials.apiKey(),
                credentials.cloudName(),
                credentials.uploadUrl(resourceType),
                folder,
                type);
    }
}
