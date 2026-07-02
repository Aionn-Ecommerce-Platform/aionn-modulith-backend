package com.aionn.identity.application.dto.media.result;

public record UploadSignatureResult(
                String signature,
                String timestamp,
                String apiKey,
                String cloudName,
                String uploadUrl,
                String folder) {
}

