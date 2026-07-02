package com.aionn.identity.application.port.in.kyc;

import com.aionn.identity.application.dto.kyc.query.GetKycQuery;
import com.aionn.identity.application.dto.kyc.result.KycResult;

public interface GetKycQueryPort {
    KycResult execute(GetKycQuery query);
}


