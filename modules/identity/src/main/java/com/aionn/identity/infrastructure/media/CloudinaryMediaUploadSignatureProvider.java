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

    private static final String AVATAR_FORMATS = "jpg,jpeg,png,webp,gif";
    private static final long AVATAR_MAX_BYTES = 5L * 1024 * 1024;
    private static final String KYC_FORMATS = "jpg,jpeg,png,webp,pdf";
    private static final long KYC_MAX_BYTES = 10L * 1024 * 1024;
    private static final String RETURN_EVIDENCE_FORMATS =
            "jpg,jpeg,png,webp,gif,mp4,mov,webm";
    private static final long RETURN_EVIDENCE_MAX_BYTES = 50L * 1024 * 1024;

    @Override
    public UploadSignatureResult generateAvatarUploadSignature(String userId) {
        return sign(folders.avatarFolder() + "/" + userId, "image", "upload",
                AVATAR_FORMATS, AVATAR_MAX_BYTES);
    }

    @Override
    public UploadSignatureResult generateKycDocumentUploadSignature(String userId) {
        return sign(folders.kycFolder() + "/" + userId, "auto", "private",
                KYC_FORMATS, KYC_MAX_BYTES);
    }

    @Override
    public UploadSignatureResult generateReturnEvidenceUploadSignature(String userId) {
        return sign(folders.returnEvidenceFolder() + "/" + userId, "auto", "upload",
                RETURN_EVIDENCE_FORMATS, RETURN_EVIDENCE_MAX_BYTES);
    }

    private UploadSignatureResult sign(String folder, String resourceType, String type,
            String allowedFormats, long maxFileSize) {
        long timestamp = clock.instant().getEpochSecond();
        Map<String, String> params = new TreeMap<>();
        params.put("folder", folder);
        params.put("timestamp", String.valueOf(timestamp));
        params.put("type", type);
        params.put("allowed_formats", allowedFormats);
        String signature = CloudinarySigner.sign(params, credentials.apiSecret());
        log.debug("Generated {} upload signature", resourceType);
        return new UploadSignatureResult(
                signature,
                String.valueOf(timestamp),
                credentials.apiKey(),
                credentials.cloudName(),
                credentials.uploadUrl(resourceType),
                folder,
                type,
                allowedFormats,
                maxFileSize);
    }
}
