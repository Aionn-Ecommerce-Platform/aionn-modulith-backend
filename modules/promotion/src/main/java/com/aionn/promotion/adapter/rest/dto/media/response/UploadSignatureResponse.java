package com.aionn.promotion.adapter.rest.dto.media.response;

public record UploadSignatureResponse(
        String signature,
        String timestamp,
        String apiKey,
        String cloudName,
        String uploadUrl,
        String folder) {
}
