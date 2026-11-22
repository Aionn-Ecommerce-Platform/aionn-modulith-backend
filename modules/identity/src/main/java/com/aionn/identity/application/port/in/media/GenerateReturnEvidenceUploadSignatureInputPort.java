package com.aionn.identity.application.port.in.media;

import com.aionn.identity.application.dto.media.result.UploadSignatureResult;

public interface GenerateReturnEvidenceUploadSignatureInputPort {
    UploadSignatureResult execute(String userId);
}
