package com.aionn.identity.application.usecase.kyc;

import com.aionn.identity.application.dto.kyc.result.KycResult;
import com.aionn.identity.application.mapper.KycResultMapper;
import com.aionn.identity.application.port.in.kyc.ListMyKycQueryPort;
import com.aionn.identity.application.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMyKycUseCase implements ListMyKycQueryPort {

    private final KycService kycService;
    private final KycResultMapper kycResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<KycResult> execute(String userId) {
        var entities = kycService.listMy(userId);
        return entities.stream()
                .map(kycResultMapper::toResult)
                .toList();
    }
}

