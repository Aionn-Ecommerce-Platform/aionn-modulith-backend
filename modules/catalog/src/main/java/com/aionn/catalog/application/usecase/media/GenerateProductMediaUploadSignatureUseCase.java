package com.aionn.catalog.application.usecase.media;

import com.aionn.catalog.application.dto.media.result.UploadSignatureResult;
import com.aionn.catalog.application.port.in.media.GenerateProductMediaUploadSignatureInputPort;
import com.aionn.catalog.application.port.out.media.ProductMediaUploadSignatureProviderPort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenerateProductMediaUploadSignatureUseCase
        implements GenerateProductMediaUploadSignatureInputPort {

    private final ProductMediaUploadSignatureProviderPort provider;
    private final MerchantQueryPort merchantQueryPort;

    @Override
    public UploadSignatureResult execute(String ownerId) {
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.MERCHANT_NOT_FOUND,
                        "No merchant registered for the authenticated user"));
        return provider.generateProductImageUploadSignature(merchantId);
    }

    @Override
    public UploadSignatureResult executeReview(String userId) {
        return provider.generateReviewImageUploadSignature(userId);
    }
}
