package com.aionn.identity.application.usecase.kyc;

import com.aionn.identity.application.dto.kyc.result.KycResult;
import com.aionn.identity.application.mapper.KycResultMapper;
import com.aionn.identity.application.port.in.kyc.ListAdminKycQueryPort;
import com.aionn.identity.application.service.KycService;
import com.aionn.identity.domain.valueobject.KycStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListAdminKycUseCase implements ListAdminKycQueryPort {

    private final KycService kycService;
    private final KycResultMapper kycResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<KycResult> execute(KycStatus status, int limit) {
        return kycService.adminListByStatus(status, limit).stream()
                .map(kycResultMapper::toResult)
                .toList();
    }
}
