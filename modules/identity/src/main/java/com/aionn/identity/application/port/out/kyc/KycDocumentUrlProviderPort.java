package com.aionn.identity.application.port.out.kyc;

public interface KycDocumentUrlProviderPort {

    String signedDownloadUrl(String publicId, String storedUrl);
}
