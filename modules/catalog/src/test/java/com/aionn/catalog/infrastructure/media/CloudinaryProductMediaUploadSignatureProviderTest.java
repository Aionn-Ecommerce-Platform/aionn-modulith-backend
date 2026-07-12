package com.aionn.catalog.infrastructure.media;

import com.aionn.catalog.application.dto.media.result.UploadSignatureResult;
import com.aionn.catalog.infrastructure.config.properties.CatalogCloudinaryProperties;
import com.aionn.sharedkernel.media.CloudinaryCredentialsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryProductMediaUploadSignatureProviderTest {

    private static final String MERCHANT_ID = "01HZMER0000000000000000001";
    private static final String USER_ID = "01HZUSR0000000000000000001";

    private CloudinaryProductMediaUploadSignatureProvider provider;

    @BeforeEach
    void setUp() {
        CloudinaryCredentialsProperties credentials = new CloudinaryCredentialsProperties(
                "demo-cloud", "api-key", "api-secret", "https://api.cloudinary.com/v1_1");
        CatalogCloudinaryProperties folders = new CatalogCloudinaryProperties(
                "aionn/catalog/products", "aionn/catalog/reviews");
        provider = new CloudinaryProductMediaUploadSignatureProvider(credentials, folders, java.time.Clock.systemUTC());
    }

    @Test
    void generatesProductImageSignatureScopedToMerchantFolder() {
        UploadSignatureResult result = provider.generateProductImageUploadSignature(MERCHANT_ID);

        assertThat(result.folder()).isEqualTo("aionn/catalog/products/" + MERCHANT_ID);
        assertThat(result.apiKey()).isEqualTo("api-key");
        assertThat(result.cloudName()).isEqualTo("demo-cloud");
        assertThat(result.uploadUrl()).isEqualTo("https://api.cloudinary.com/v1_1/demo-cloud/image/upload");
        assertThat(result.signature()).isNotBlank();
        assertThat(result.timestamp()).isNotBlank();
    }

    @Test
    void generatesReviewImageSignatureScopedToUserFolder() {
        UploadSignatureResult result = provider.generateReviewImageUploadSignature(USER_ID);

        assertThat(result.folder()).isEqualTo("aionn/catalog/reviews/" + USER_ID);
        assertThat(result.signature()).isNotBlank();
    }
}
