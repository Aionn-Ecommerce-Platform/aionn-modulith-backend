package com.aionn.catalog.application.usecase.media;

import com.aionn.catalog.application.dto.media.result.UploadSignatureResult;
import com.aionn.catalog.application.port.out.media.ProductMediaUploadSignatureProviderPort;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateProductMediaUploadSignatureUseCaseTest {

    private static final String OWNER_ID = "01HZOWN0000000000000000001";
    private static final String MERCHANT_ID = "01HZMER0000000000000000001";

    @Mock
    private ProductMediaUploadSignatureProviderPort provider;

    @Mock
    private MerchantQueryPort merchantQueryPort;

    @InjectMocks
    private GenerateProductMediaUploadSignatureUseCase useCase;

    @Test
    void executeResolvesMerchantAndDelegatesToProvider() {
        UploadSignatureResult expected = new UploadSignatureResult(
                "sig", "123", "key", "cloud", "https://upload", "folder");
        when(merchantQueryPort.findMerchantIdByOwnerId(OWNER_ID)).thenReturn(Optional.of(MERCHANT_ID));
        when(provider.generateProductImageUploadSignature(MERCHANT_ID)).thenReturn(expected);

        UploadSignatureResult result = useCase.execute(OWNER_ID);

        assertThat(result).isEqualTo(expected);
        verify(provider).generateProductImageUploadSignature(MERCHANT_ID);
    }

    @Test
    void executeThrowsWhenOwnerHasNoMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(OWNER_ID))
                .isInstanceOf(CatalogException.class);

        verify(provider, never()).generateProductImageUploadSignature(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void executeReviewDelegatesWithUserId() {
        UploadSignatureResult expected = new UploadSignatureResult(
                "sig", "123", "key", "cloud", "https://upload", "reviews/user");
        when(provider.generateReviewImageUploadSignature(OWNER_ID)).thenReturn(expected);

        UploadSignatureResult result = useCase.executeReview(OWNER_ID);

        assertThat(result).isEqualTo(expected);
        verify(provider).generateReviewImageUploadSignature(OWNER_ID);
    }
}
