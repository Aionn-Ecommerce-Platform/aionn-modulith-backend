package com.aionn.identity.application.port.in.kyc;

import com.aionn.identity.application.dto.kyc.result.KycResult;
import com.aionn.identity.domain.valueobject.KycStatus;

import java.util.List;

public interface ListAdminKycQueryPort {

    List<KycResult> execute(KycStatus status, int limit);
}
