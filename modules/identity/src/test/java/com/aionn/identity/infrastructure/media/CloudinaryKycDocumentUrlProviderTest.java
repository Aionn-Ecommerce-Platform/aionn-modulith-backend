package com.aionn.identity.infrastructure.media;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryKycDocumentUrlProviderTest {

    @Mock
    private Cloudinary cloudinary;

    private CloudinaryKycDocumentUrlProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CloudinaryKycDocumentUrlProvider(cloudinary);
    }

    @Test
    void returnsNullWithoutAPublicId() {
        assertThat(provider.signedDownloadUrl(null, null)).isNull();
        assertThat(provider.signedDownloadUrl("  ", "https://x/image/upload/a")).isNull();
    }

    @Test
    void signsImages() throws Exception {
        when(cloudinary.privateDownload(eq("kyc/1"), eq("image"), anyMap()))
                .thenReturn("https://signed/image");

        assertThat(provider.signedDownloadUrl("kyc/1", "https://x/image/upload/a"))
                .isEqualTo("https://signed/image");
    }

    @Test
    void signsRawDocuments() throws Exception {
        when(cloudinary.privateDownload(eq("kyc/1"), eq("raw"), anyMap()))
                .thenReturn("https://signed/raw");

        assertThat(provider.signedDownloadUrl("kyc/1", "https://x/raw/upload/a"))
                .isEqualTo("https://signed/raw");
    }

    @Test
    void wrapsSigningFailures() throws Exception {
        when(cloudinary.privateDownload(any(), any(), anyMap()))
                .thenThrow(new Exception("boom"));

        assertThatThrownBy(() -> provider.signedDownloadUrl("kyc/1", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("signed KYC document URL");
    }
}
