package com.aionn.promotion.application.dto.media.result;

public record UploadSignatureResult(
        String signature,
        String timestamp,
        String apiKey,
        String cloudName,
        String uploadUrl,
        String folder) {
}
