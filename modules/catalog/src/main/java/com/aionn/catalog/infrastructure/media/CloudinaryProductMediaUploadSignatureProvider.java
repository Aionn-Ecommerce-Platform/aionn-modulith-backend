package com.aionn.catalog.infrastructure.media;

import com.aionn.catalog.application.dto.media.result.UploadSignatureResult;
import com.aionn.catalog.application.port.out.media.ProductMediaUploadSignatureProviderPort;
import com.aionn.catalog.infrastructure.config.properties.CatalogCloudinaryProperties;
import com.aionn.sharedkernel.media.CloudinaryCredentialsProperties;
import com.aionn.sharedkernel.media.CloudinarySigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
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

    @Override
    public UploadSignatureResult generateProductImageUploadSignature(String merchantId) {
        return sign(folders.productImageFolder() + "/" + merchantId, "product-image");
    }

    @Override
    public UploadSignatureResult generateReviewImageUploadSignature(String userId) {
        return sign(folders.reviewImageFolder() + "/" + userId, "review-image");
    }

    private UploadSignatureResult sign(String folder, String label) {
        long timestamp = Instant.now(Clock.systemUTC()).getEpochSecond();

        Map<String, String> params = new TreeMap<>();
        params.put("folder", folder);
        params.put("timestamp", String.valueOf(timestamp));

        String signature = CloudinarySigner.sign(params, credentials.apiSecret());

        log.debug("Generated {} upload signature, folder={}", label, folder);
        return new UploadSignatureResult(
                signature,
                String.valueOf(timestamp),
                credentials.apiKey(),
                credentials.cloudName(),
                credentials.uploadUrl("image"),
                folder);
    }
}
