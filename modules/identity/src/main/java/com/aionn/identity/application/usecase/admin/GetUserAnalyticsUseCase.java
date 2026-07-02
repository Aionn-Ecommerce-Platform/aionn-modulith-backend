package com.aionn.identity.application.usecase.admin;

import com.aionn.identity.application.dto.analytics.result.UserAnalyticsResult;
import com.aionn.identity.application.port.in.admin.GetUserAnalyticsQueryPort;
import com.aionn.identity.application.port.out.analytics.UserAnalyticsQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetUserAnalyticsUseCase implements GetUserAnalyticsQueryPort {

    private final UserAnalyticsQueryPort userAnalyticsQueryPort;

    @Override
    @Transactional(readOnly = true)
    public UserAnalyticsResult execute(LocalDate from, LocalDate to) {
        return userAnalyticsQueryPort.getUserAnalytics(from, to);
    }
}
