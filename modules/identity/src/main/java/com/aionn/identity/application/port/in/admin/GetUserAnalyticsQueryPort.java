package com.aionn.identity.application.port.in.admin;

import com.aionn.identity.application.dto.analytics.result.UserAnalyticsResult;

import java.time.LocalDate;

public interface GetUserAnalyticsQueryPort {

    UserAnalyticsResult execute(LocalDate from, LocalDate to);
}
