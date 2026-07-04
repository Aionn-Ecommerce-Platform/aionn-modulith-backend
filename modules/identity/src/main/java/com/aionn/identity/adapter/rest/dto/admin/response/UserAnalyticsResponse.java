package com.aionn.identity.adapter.rest.dto.admin.response;

import java.time.LocalDate;
import java.util.List;

public record UserAnalyticsResponse(
        LocalDate from,
        LocalDate to,
        long totalUsers,
        long newUsersInRange,
        List<DailySignup> signupTrend,
        List<RoleCount> roleBreakdown,
        List<StatusCount> statusBreakdown) {

    public record DailySignup(LocalDate date, long count) {
    }

    public record RoleCount(String role, long count) {
    }

    public record StatusCount(String status, long count) {
    }
}
