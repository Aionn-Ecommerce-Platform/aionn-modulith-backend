package com.aionn.catalog.infrastructure.media;

import com.aionn.catalog.application.dto.media.result.UploadSignatureResult;
import com.aionn.catalog.application.port.out.media.ProductMediaUploadSignatureProviderPort;
import com.aionn.catalog.infrastructure.config.properties.CatalogCloudinaryProperties;
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
@ConditionalOnProperty(prefix = "catalog.media", name = "provider", havingValue = "cloudinary", matchIfMissing = true)
public class CloudinaryProductMediaUploadSignatureProvider
        implements ProductMediaUploadSignatureProviderPort {

    private final CloudinaryCredentialsProperties credentials;
    private final CatalogCloudinaryProperties folders;
    private final Clock clock;

    @Override
    public UploadSignatureResult generateProductImageUploadSignature(String merchantId) {
        // "auto" lets Cloudinary accept both images and videos for product media.
        return sign(folders.productImageFolder() + "/" + merchantId, "product-media",
                "auto", "jpg,jpeg,png,webp,mp4,mov,webm", 50L * 1024 * 1024);
    }

    @Override
    public UploadSignatureResult generateReviewImageUploadSignature(String userId) {
        return sign(folders.reviewImageFolder() + "/" + userId, "review-image",
                "auto", "jpg,jpeg,png,webp,gif,mp4,mov,webm", 50L * 1024 * 1024);
    }

    private UploadSignatureResult sign(String folder, String label, String resourceType,
            String allowedFormats, long maxFileSize) {
        long timestamp = clock.instant().getEpochSecond();

        Map<String, String> params = new TreeMap<>();
        params.put("folder", folder);
        params.put("timestamp", String.valueOf(timestamp));
        params.put("allowed_formats", allowedFormats);

        String signature = CloudinarySigner.sign(params, credentials.apiSecret());

        log.debug("Generated {} upload signature, folder={}", label, folder);
        return new UploadSignatureResult(
                signature,
                String.valueOf(timestamp),
                credentials.apiKey(),
                credentials.cloudName(),
                credentials.uploadUrl(resourceType),
                folder,
                allowedFormats,
                maxFileSize);
    }
}
