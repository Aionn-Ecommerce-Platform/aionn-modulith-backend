package com.aionn.identity.application.usecase.kyc;

import com.aionn.identity.application.dto.kyc.result.KycVerificationSessionResult;
import com.aionn.identity.application.port.in.kyc.GenerateKycVerificationSessionInputPort;
import com.aionn.identity.application.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenerateKycVerificationSessionUseCase implements GenerateKycVerificationSessionInputPort {

    private final KycService kycService;

    @Override
    public KycVerificationSessionResult execute(String userId, String kycId) {
        return kycService.generateVerificationSession(userId, kycId);
    }
}
