package com.aionn.identity.infrastructure.media;

import com.aionn.identity.application.port.out.kyc.KycDocumentUrlProviderPort;
import com.aionn.platform.media.CloudinaryCredentialsProperties;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "identity.media", name = "provider", havingValue = "cloudinary", matchIfMissing = true)
public class CloudinaryKycDocumentUrlProvider implements KycDocumentUrlProviderPort {

    private final Cloudinary cloudinary;

    public CloudinaryKycDocumentUrlProvider(CloudinaryCredentialsProperties credentials) {
        this(new Cloudinary(ObjectUtils.asMap(
                "cloud_name", credentials.cloudName(),
                "api_key", credentials.apiKey(),
                "api_secret", credentials.apiSecret())));
    }

    CloudinaryKycDocumentUrlProvider(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String signedDownloadUrl(String publicId, String storedUrl) {
        if (publicId == null || publicId.isBlank()) {
            return null;
        }
        String resourceType = storedUrl != null && storedUrl.contains("/raw/") ? "raw" : "image";
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("type", "private");
            options.put("attachment", false);
            return cloudinary.privateDownload(publicId, resourceType, options);
        } catch (Exception ex) {
            log.warn("Failed to sign KYC document URL for asset {}", publicId, ex);
            throw new IllegalStateException("Failed to generate signed KYC document URL", ex);
        }
    }
}
