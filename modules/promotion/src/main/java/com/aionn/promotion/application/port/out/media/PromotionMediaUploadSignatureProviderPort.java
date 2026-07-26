package com.aionn.promotion.application.port.out.media;

import com.aionn.promotion.application.dto.media.result.UploadSignatureResult;

public interface PromotionMediaUploadSignatureProviderPort {

    UploadSignatureResult generateBannerUploadSignature();
}
