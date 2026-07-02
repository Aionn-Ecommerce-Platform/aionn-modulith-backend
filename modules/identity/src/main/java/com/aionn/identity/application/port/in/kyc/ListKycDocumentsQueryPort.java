package com.aionn.identity.application.port.in.kyc;

import com.aionn.identity.application.dto.kyc.result.KycDocumentResult;

import java.util.List;

public interface ListKycDocumentsQueryPort {
    List<KycDocumentResult> execute(String userId, String kycId);
}
