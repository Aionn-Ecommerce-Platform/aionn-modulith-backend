package com.aionn.identity.application.port.in.kyc;

import com.aionn.identity.application.dto.analytics.result.KycAnalyticsResult;

import java.time.LocalDate;

public interface GetKycAnalyticsQueryPort {

    KycAnalyticsResult execute(LocalDate from, LocalDate to);
}
