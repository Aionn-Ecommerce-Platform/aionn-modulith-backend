package com.aionn.identity.application.usecase.kyc;

import com.aionn.identity.application.dto.kyc.result.KycResult;
import com.aionn.identity.application.mapper.KycResultMapper;
import com.aionn.identity.application.port.in.kyc.SubmitKycInputPort;
import com.aionn.identity.application.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmitKycUseCase implements SubmitKycInputPort {

    private final KycService kycService;
    private final KycResultMapper mapper;

    @Override
    @Transactional
    public KycResult execute(String userId, String kycId) {
        return mapper.toResult(kycService.submit(userId, kycId));
    }
}
