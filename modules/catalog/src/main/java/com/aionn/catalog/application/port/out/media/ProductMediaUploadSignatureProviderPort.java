package com.aionn.catalog.application.port.out.media;

import com.aionn.catalog.application.dto.media.result.UploadSignatureResult;

public interface ProductMediaUploadSignatureProviderPort {

    UploadSignatureResult generateProductImageUploadSignature(String merchantId);

    UploadSignatureResult generateReviewImageUploadSignature(String userId);
}
