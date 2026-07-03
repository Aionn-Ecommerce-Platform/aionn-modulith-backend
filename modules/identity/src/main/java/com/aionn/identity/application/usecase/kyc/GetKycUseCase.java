package com.aionn.identity.application.usecase.kyc;

import com.aionn.identity.application.dto.kyc.query.GetKycQuery;
import com.aionn.identity.application.dto.kyc.result.KycResult;
import com.aionn.identity.application.mapper.KycResultMapper;
import com.aionn.identity.application.port.in.kyc.GetKycQueryPort;
import com.aionn.identity.application.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetKycUseCase implements GetKycQueryPort {

    private final KycService kycService;
    private final KycResultMapper kycResultMapper;

    @Override
    @Transactional(readOnly = true)
    public KycResult execute(GetKycQuery query) {
        var entity = kycService.get(query.userId(), query.kycId());
        return kycResultMapper.toResult(entity);
    }
}

