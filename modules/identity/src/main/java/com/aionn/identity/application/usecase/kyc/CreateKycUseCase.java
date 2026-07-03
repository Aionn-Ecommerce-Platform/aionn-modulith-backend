package com.aionn.identity.application.usecase.kyc;

import com.aionn.identity.application.dto.kyc.result.KycResult;
import com.aionn.identity.application.dto.kyc.command.CreateKycCommand;
import com.aionn.identity.application.mapper.KycResultMapper;
import com.aionn.identity.application.port.in.kyc.CreateKycInputPort;
import com.aionn.identity.application.port.out.kyc.ExternalKycVerificationPort;
import com.aionn.identity.application.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateKycUseCase implements CreateKycInputPort {

    private final KycService kycService;
    private final ExternalKycVerificationPort externalKycVerificationPort;
    private final KycResultMapper kycResultMapper;

    @Override
    public KycResult execute(CreateKycCommand command) {
        var plan = kycService.planKycCreation(command.userId(), command.docType());
        ExternalKycVerificationPort.ExternalKycApplicant applicant = null;
        if (plan.managedProviderEnabled()) {
            applicant = externalKycVerificationPort.createApplicant(
                    plan.user(),
                    plan.kycId(),
                    plan.docType());
        }
        var entity = kycService.createKyc(plan, applicant);
        return kycResultMapper.toResult(entity);
    }
}

