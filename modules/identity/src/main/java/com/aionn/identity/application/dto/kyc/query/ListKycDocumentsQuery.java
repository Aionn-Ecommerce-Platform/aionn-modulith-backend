package com.aionn.identity.application.dto.kyc.query;

import com.aionn.sharedkernel.application.query.Query;

public record ListKycDocumentsQuery(
        String userId,
        String kycId) implements Query {
}
