package com.aionn.identity.infrastructure.media;

import com.aionn.identity.application.port.out.kyc.KycDocumentStoragePort;
import com.aionn.identity.infrastructure.config.properties.CloudinaryProperties;
import com.aionn.platform.media.CloudinaryCredentialsProperties;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "identity.media", name = "provider", havingValue = "cloudinary", matchIfMissing = true)
public class CloudinaryKycDocumentStorageAdapter implements KycDocumentStoragePort {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties folders;

    @org.springframework.beans.factory.annotation.Autowired
    public CloudinaryKycDocumentStorageAdapter(
            CloudinaryCredentialsProperties credentials,
            CloudinaryProperties folders) {
        this(new Cloudinary(ObjectUtils.asMap(
                "cloud_name", credentials.cloudName(),
                "api_key", credentials.apiKey(),
                "api_secret", credentials.apiSecret())), folders);
    }

    CloudinaryKycDocumentStorageAdapter(Cloudinary cloudinary, CloudinaryProperties folders) {
        this.cloudinary = cloudinary;
        this.folders = folders;
    }

    @Override
    public boolean isOwnedBy(String publicId, String userId) {
        if (publicId == null || publicId.isBlank() || userId == null || userId.isBlank()) {
            return false;
        }
        return publicId.startsWith(folders.kycFolder() + "/" + userId + "/");
    }

    @Override
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        boolean found = destroy(publicId, "image", "private")
                || destroy(publicId, "raw", "private")
                // Legacy documents uploaded before KYC uploads became private.
                || destroy(publicId, "image", "upload")
                || destroy(publicId, "raw", "upload");
        if (!found) {
            log.debug("KYC asset {} was already absent from Cloudinary", publicId);
        }
    }

    private boolean destroy(String publicId, String resourceType, String type) {
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "type", type,
                    "invalidate", true));
            Object outcome = result == null ? null : result.get("result");
            if ("ok".equals(outcome)) {
                log.info("Deleted KYC asset {} ({}/{})", publicId, resourceType, type);
                return true;
            }
            return false;
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to delete KYC asset " + publicId + " (" + resourceType + "/" + type + ")", ex);
        }
    }
}
