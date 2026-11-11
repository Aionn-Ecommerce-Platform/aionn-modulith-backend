package com.aionn.identity.infrastructure.media;

import com.aionn.identity.infrastructure.config.properties.CloudinaryProperties;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryKycDocumentStorageAdapterTest {

    private static final String FOLDER = "identity/kyc";

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;

    private CloudinaryKycDocumentStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CloudinaryKycDocumentStorageAdapter(cloudinary,
                new CloudinaryProperties("identity/avatars", FOLDER));
    }

    @Test
    void ownershipRequiresTheFolderAndUserPrefix() {
        assertThat(adapter.isOwnedBy(FOLDER + "/user-1/doc", "user-1")).isTrue();
        assertThat(adapter.isOwnedBy(FOLDER + "/user-1/sub/back", "user-1")).isTrue();
        assertThat(adapter.isOwnedBy(FOLDER + "/user-2/doc", "user-1")).isFalse();
        assertThat(adapter.isOwnedBy("identity/avatars/user-1/avatar", "user-1")).isFalse();
        assertThat(adapter.isOwnedBy(FOLDER + "/user-10/front", "user-1")).isFalse();
        assertThat(adapter.isOwnedBy(null, "user-1")).isFalse();
        assertThat(adapter.isOwnedBy("  ", "user-1")).isFalse();
        assertThat(adapter.isOwnedBy(FOLDER + "/user-1/doc", null)).isFalse();
        assertThat(adapter.isOwnedBy(FOLDER + "/user-1/doc", " ")).isFalse();
    }

    @Test
    void deleteIgnoresBlankIds() {
        adapter.delete(null);
        adapter.delete(" ");

        verify(cloudinary, never()).uploader();
    }

    @Test
    void deleteStopsAtTheFirstSuccessfulVariant() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("kyc/1"), anyMap())).thenReturn(Map.of("result", "ok"));

        adapter.delete("kyc/1");

        verify(uploader, times(1)).destroy(eq("kyc/1"), anyMap());
    }

    @Test
    void deleteTriesEveryVariantWhenTheAssetIsElsewhere() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("kyc/1"), anyMap()))
                .thenReturn(Map.of("result", "not found"))
                .thenReturn(Map.of("result", "ok"));

        adapter.delete("kyc/1");

        verify(uploader, times(2)).destroy(eq("kyc/1"), anyMap());
    }

    @Test
    void deleteToleratesAnAlreadyMissingAsset() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("kyc/1"), anyMap())).thenReturn(Map.of("result", "not found"));

        adapter.delete("kyc/1");

        verify(uploader, times(4)).destroy(eq("kyc/1"), anyMap());
    }

    @Test
    void deleteWrapsProviderFailures() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(any(), anyMap())).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> adapter.delete("kyc/1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to delete KYC asset");
    }

    @Test
    void deleteHandlesANullProviderResponse() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("kyc/1"), anyMap())).thenReturn(null);

        adapter.delete("kyc/1");

        verify(uploader, times(4)).destroy(eq("kyc/1"), anyMap());
    }
}
