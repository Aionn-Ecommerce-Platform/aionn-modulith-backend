package com.aionn.identity.infrastructure.media;

import com.aionn.identity.infrastructure.config.properties.CloudinaryProperties;
import com.aionn.platform.media.CloudinaryCredentialsProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryKycDocumentStorageAdapterTest {

    private final CloudinaryKycDocumentStorageAdapter adapter =
            new CloudinaryKycDocumentStorageAdapter(
                    new CloudinaryCredentialsProperties("cloud", "key", "secret",
                            "https://api.cloudinary.com/v1_1"),
                    new CloudinaryProperties("identity/avatars", "identity/kyc"));

    @Test
    void acceptsAssetsInsideTheUsersKycFolder() {
        assertThat(adapter.isOwnedBy("identity/kyc/user-1/front", "user-1")).isTrue();
        assertThat(adapter.isOwnedBy("identity/kyc/user-1/sub/back", "user-1")).isTrue();
    }

    @Test
    void rejectsAssetsOutsideTheUsersKycFolder() {
        assertThat(adapter.isOwnedBy("identity/kyc/user-2/front", "user-1")).isFalse();
        assertThat(adapter.isOwnedBy("identity/avatars/user-1/avatar", "user-1")).isFalse();
        assertThat(adapter.isOwnedBy("identity/kyc/user-10/front", "user-1")).isFalse();
        assertThat(adapter.isOwnedBy(null, "user-1")).isFalse();
        assertThat(adapter.isOwnedBy("identity/kyc/user-1/front", null)).isFalse();
    }
}
