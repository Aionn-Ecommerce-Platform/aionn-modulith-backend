package com.aionn.chat.application.port.in.media;

import com.aionn.chat.application.dto.media.result.UploadSignatureResult;

public interface GenerateChatMediaUploadSignatureInputPort {
    UploadSignatureResult execute(String userId);
}
