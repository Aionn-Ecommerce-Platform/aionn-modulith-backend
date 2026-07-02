package com.aionn.identity.application.usecase.kyc;

import com.aionn.identity.application.dto.kyc.result.KycResult;
import com.aionn.identity.application.dto.kyc.command.CreateKycCommand;
import com.aionn.identity.application.mapper.KycResultMapper;
import com.aionn.identity.application.port.in.kyc.CreateKycInputPort;
import com.aionn.identity.application.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateKycUseCase implements CreateKycInputPort {

    private final KycService kycService;
    private final KycResultMapper kycResultMapper;

    @Override
    @Transactional
    public KycResult execute(CreateKycCommand command) {
        var entity = kycService.createKyc(command.userId(), command.docType());
        return kycResultMapper.toResult(entity);
    }
}

