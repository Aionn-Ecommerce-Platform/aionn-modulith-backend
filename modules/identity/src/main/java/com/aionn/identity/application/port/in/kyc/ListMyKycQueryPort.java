package com.aionn.identity.application.port.in.kyc;

import com.aionn.identity.application.dto.kyc.result.KycResult;
import java.util.List;

public interface ListMyKycQueryPort {
    List<KycResult> execute(String userId);
}
