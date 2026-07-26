package com.aionn.promotion.infrastructure.media;

import com.aionn.promotion.application.dto.media.result.UploadSignatureResult;
import com.aionn.promotion.application.port.out.media.PromotionMediaUploadSignatureProviderPort;
import com.aionn.promotion.infrastructure.config.properties.PromotionCloudinaryProperties;
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
@ConditionalOnProperty(prefix = "promotion.media", name = "provider", havingValue = "cloudinary",
        matchIfMissing = true)
public class CloudinaryPromotionMediaUploadSignatureProvider
        implements PromotionMediaUploadSignatureProviderPort {

    private final CloudinaryCredentialsProperties credentials;
    private final PromotionCloudinaryProperties folders;

    @Override
    public UploadSignatureResult generateBannerUploadSignature() {
        String folder = folders.bannerFolder();
        long timestamp = Instant.now().getEpochSecond();

        Map<String, String> params = new TreeMap<>();
        params.put("folder", folder);
        params.put("timestamp", String.valueOf(timestamp));
        params.put("eager", "c_fill,w_1600,h_600");

        String signature = CloudinarySigner.sign(params, credentials.apiSecret());

        log.debug("Generated promotion-banner upload signature, folder={}", folder);
        return new UploadSignatureResult(
                signature,
                String.valueOf(timestamp),
                credentials.apiKey(),
                credentials.cloudName(),
                credentials.uploadUrl("image"),
                folder);
    }
}
