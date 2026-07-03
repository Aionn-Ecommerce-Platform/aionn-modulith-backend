package com.aionn.identity.application.usecase.kyc;

import com.aionn.identity.application.dto.analytics.result.KycAnalyticsResult;
import com.aionn.identity.application.port.in.kyc.GetKycAnalyticsQueryPort;
import com.aionn.identity.application.port.out.analytics.KycAnalyticsQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetKycAnalyticsUseCase implements GetKycAnalyticsQueryPort {

    private final KycAnalyticsQueryPort kycAnalyticsQueryPort;

    @Override
    @Transactional(readOnly = true)
    public KycAnalyticsResult execute(LocalDate from, LocalDate to) {
        return kycAnalyticsQueryPort.getKycAnalytics(from, to);
    }
}
