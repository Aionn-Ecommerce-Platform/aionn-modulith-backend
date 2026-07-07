package com.aionn.catalog.application.port.in.media;

import com.aionn.catalog.application.dto.media.result.UploadSignatureResult;

public interface GenerateProductMediaUploadSignatureInputPort {

    UploadSignatureResult execute(String ownerId);

    UploadSignatureResult executeReview(String userId);
}
