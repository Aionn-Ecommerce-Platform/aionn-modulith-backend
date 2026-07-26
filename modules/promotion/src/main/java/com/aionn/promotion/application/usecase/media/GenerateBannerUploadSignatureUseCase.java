package com.aionn.promotion.application.usecase.media;

import com.aionn.promotion.application.dto.media.result.UploadSignatureResult;
import com.aionn.promotion.application.port.in.media.GenerateBannerUploadSignatureInputPort;
import com.aionn.promotion.application.port.out.media.PromotionMediaUploadSignatureProviderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenerateBannerUploadSignatureUseCase implements GenerateBannerUploadSignatureInputPort {

    private final PromotionMediaUploadSignatureProviderPort provider;

    @Override
    public UploadSignatureResult execute() {
        return provider.generateBannerUploadSignature();
    }
}
