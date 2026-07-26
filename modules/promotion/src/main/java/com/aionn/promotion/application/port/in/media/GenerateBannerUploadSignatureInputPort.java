package com.aionn.promotion.application.port.in.media;

import com.aionn.promotion.application.dto.media.result.UploadSignatureResult;

public interface GenerateBannerUploadSignatureInputPort {
    UploadSignatureResult execute();
}
