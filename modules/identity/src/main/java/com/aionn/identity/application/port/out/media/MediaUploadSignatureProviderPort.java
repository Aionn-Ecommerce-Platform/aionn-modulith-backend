package com.aionn.identity.application.port.out.media;

import com.aionn.identity.application.dto.media.result.UploadSignatureResult;

public interface MediaUploadSignatureProviderPort {

    UploadSignatureResult generateAvatarUploadSignature(String userId);

    UploadSignatureResult generateKycDocumentUploadSignature(String userId);
}
