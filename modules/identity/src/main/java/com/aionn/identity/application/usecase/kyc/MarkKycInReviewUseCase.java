package com.aionn.identity.application.usecase.kyc;

import com.aionn.identity.application.dto.kyc.command.KycAdminCommands;
import com.aionn.identity.application.dto.kyc.result.KycResult;
import com.aionn.identity.application.mapper.KycResultMapper;
import com.aionn.identity.application.port.in.kyc.MarkKycInReviewInputPort;
import com.aionn.identity.application.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarkKycInReviewUseCase implements MarkKycInReviewInputPort {

    private final KycService kycService;
    private final KycResultMapper kycResultMapper;

    @Override
    @Transactional
    public KycResult execute(KycAdminCommands.MarkInReviewKyc command) {
        return kycResultMapper.toResult(
                kycService.adminMarkInReview(command.kycId(), command.adminId(), command.note()));
    }
}
