package com.aionn.identity.application.port.out.kyc;

public interface KycDocumentStoragePort {

    void delete(String publicId);

    /**
     * Verifies that the provider asset belongs to the user's own KYC folder. Trusting a
     * client-supplied public id would let one account attach (and later delete through the
     * account-deletion flow) another user's document.
     */
    boolean isOwnedBy(String publicId, String userId);
}
