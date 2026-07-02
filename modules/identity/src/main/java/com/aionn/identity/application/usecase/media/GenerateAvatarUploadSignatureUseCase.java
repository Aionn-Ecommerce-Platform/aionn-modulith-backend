package com.aionn.identity.application.usecase.media;

import com.aionn.identity.application.dto.media.result.UploadSignatureResult;
import com.aionn.identity.application.port.in.media.GenerateAvatarUploadSignatureInputPort;
import com.aionn.identity.application.port.out.media.MediaUploadSignatureProviderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenerateAvatarUploadSignatureUseCase implements GenerateAvatarUploadSignatureInputPort {

    private final MediaUploadSignatureProviderPort provider;

    @Override
    public UploadSignatureResult execute(String userId) {
        return provider.generateAvatarUploadSignature(userId);
    }
}
