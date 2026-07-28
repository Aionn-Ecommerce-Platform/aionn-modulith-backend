package com.aionn.chat.application.port.out.media;

import com.aionn.chat.application.dto.media.result.UploadSignatureResult;

public interface ChatMediaUploadSignatureProviderPort {

    UploadSignatureResult generateChatImageUploadSignature(String userId);
}
