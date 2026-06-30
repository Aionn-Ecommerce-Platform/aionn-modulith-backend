package com.aionn.sharedkernel.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudinary")
public record CloudinaryCredentialsProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        String uploadBaseUrl) {

    public String uploadUrl(String resourceType) {
        return CloudinarySigner.uploadUrl(uploadBaseUrl, cloudName, resourceType);
    }
}
