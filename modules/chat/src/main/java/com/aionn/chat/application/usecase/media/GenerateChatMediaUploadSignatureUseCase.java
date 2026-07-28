package com.aionn.chat.application.usecase.media;

import com.aionn.chat.application.dto.media.result.UploadSignatureResult;
import com.aionn.chat.application.port.in.media.GenerateChatMediaUploadSignatureInputPort;
import com.aionn.chat.application.port.out.media.ChatMediaUploadSignatureProviderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenerateChatMediaUploadSignatureUseCase
        implements GenerateChatMediaUploadSignatureInputPort {

    private final ChatMediaUploadSignatureProviderPort provider;

    @Override
    public UploadSignatureResult execute(String userId) {
        return provider.generateChatImageUploadSignature(userId);
    }
}
