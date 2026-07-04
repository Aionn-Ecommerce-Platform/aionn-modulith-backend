package com.aionn.identity.infrastructure.persistence.adapter.analytics;

import com.aionn.identity.application.dto.analytics.result.FeedbackAnalyticsResult;
import com.aionn.identity.application.dto.analytics.result.KycAnalyticsResult;
import com.aionn.identity.application.dto.analytics.result.UserAnalyticsResult;
import com.aionn.identity.application.port.out.analytics.FeedbackAnalyticsQueryPort;
import com.aionn.identity.application.port.out.analytics.KycAnalyticsQueryPort;
import com.aionn.identity.application.port.out.analytics.UserAnalyticsQueryPort;
import com.aionn.identity.infrastructure.persistence.repository.feedback.UserFeedbackRepository;
import com.aionn.identity.infrastructure.persistence.repository.kyc.KycProfileRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IdentityAnalyticsAdapter implements UserAnalyticsQueryPort, KycAnalyticsQueryPort, FeedbackAnalyticsQueryPort {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserRepository userRepository;
    private final KycProfileRepository kycRepository;
    private final UserFeedbackRepository feedbackRepository;

    @Override
    public UserAnalyticsResult getUserAnalytics(LocalDate from, LocalDate to) {
        LocalDate safeTo = to == null ? LocalDate.now(ZONE) : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(29) : from;
        validateRange(safeFrom, safeTo);
        LocalDateTime start = safeFrom.atStartOfDay();
        LocalDateTime end = safeTo.plusDays(1).atStartOfDay();

        Map<LocalDate, Long> signupByDate = new LinkedHashMap<>();
        for (LocalDate day = safeFrom; !day.isAfter(safeTo); day = day.plusDays(1)) {
            signupByDate.put(day, 0L);
        }
        var createdAts = userRepository.findCreatedAtBetween(start, end);
        for (LocalDateTime ts : createdAts) {
            LocalDate day = ts.toLocalDate();
            signupByDate.computeIfPresent(day, (k, v) -> v + 1);
        }

        long total = userRepository.count();
        var statusRows = userRepository.countByStatus();
        var roleRows = userRepository.countByRole();

        List<UserAnalyticsResult.DailySignup> trend = signupByDate.entrySet().stream()
                .map(e -> new UserAnalyticsResult.DailySignup(e.getKey(), e.getValue()))
                .toList();
        List<UserAnalyticsResult.StatusCount> statusBreakdown = statusRows.stream()
                .map(r -> new UserAnalyticsResult.StatusCount(
                        r.getStatus() == null ? "UNKNOWN" : r.getStatus().name(),
                        r.getCnt() == null ? 0 : r.getCnt()))
                .toList();
        List<UserAnalyticsResult.RoleCount> roleBreakdown = roleRows.stream()
                .map(r -> new UserAnalyticsResult.RoleCount(
                        r.getRole() == null ? "UNKNOWN" : r.getRole().name(),
                        r.getCnt() == null ? 0 : r.getCnt()))
                .toList();

        return new UserAnalyticsResult(safeFrom, safeTo, total, createdAts.size(),
                trend, roleBreakdown, statusBreakdown);
    }

    @Override
    public KycAnalyticsResult getKycAnalytics(LocalDate from, LocalDate to) {
        LocalDate safeTo = to == null ? LocalDate.now(ZONE) : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(29) : from;
        validateRange(safeFrom, safeTo);
        LocalDateTime start = safeFrom.atStartOfDay();
        LocalDateTime end = safeTo.plusDays(1).atStartOfDay();

        long pending = 0, approved = 0, rejected = 0, submitted = 0;
        for (var row : kycRepository.countByStatus()) {
            long cnt = row.getCnt() == null ? 0 : row.getCnt();
            switch (row.getStatus()) {
                case "PENDING" -> pending = cnt;
                case "APPROVED" -> approved = cnt;
                case "REJECTED" -> rejected = cnt;
                case "SUBMITTED" -> submitted = cnt;
                default -> { }
            }
        }
        double approvalRate = (approved + rejected) == 0
                ? 0.0
                : (double) approved / (approved + rejected);

        var decisions = kycRepository.findDecisionsBetween(start, end);
        double avgHours = 0.0;
        if (!decisions.isEmpty()) {
            long totalMinutes = 0;
            int counted = 0;
            for (var d : decisions) {
                if (d.getSubmittedAt() == null || d.getApprovedAt() == null) {
                    continue;
                }
                totalMinutes += Duration.between(d.getSubmittedAt(), d.getApprovedAt()).toMinutes();
                counted++;
            }
            if (counted > 0) {
                avgHours = totalMinutes / 60.0 / counted;
            }
        }
        return new KycAnalyticsResult(safeFrom, safeTo, pending, approved, rejected,
                submitted, approvalRate, avgHours);
    }

    @Override
    public FeedbackAnalyticsResult getFeedbackAnalytics(LocalDate from, LocalDate to) {
        LocalDate safeTo = to == null ? LocalDate.now(ZONE) : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(29) : from;
        validateRange(safeFrom, safeTo);
        LocalDateTime start = safeFrom.atStartOfDay();
        LocalDateTime end = safeTo.plusDays(1).atStartOfDay();

        long open = 0, resolved = 0, inReview = 0, closed = 0;
        for (var row : feedbackRepository.countByStatus()) {
            long cnt = row.getCnt() == null ? 0 : row.getCnt();
            switch (row.getStatus()) {
                case OPEN -> open = cnt;
                case RESOLVED -> resolved = cnt;
                case IN_REVIEW -> inReview = cnt;
                case CLOSED -> closed = cnt;
            }
        }
        List<FeedbackAnalyticsResult.CategoryCount> byCategory = feedbackRepository.countByCategory().stream()
                .map(r -> new FeedbackAnalyticsResult.CategoryCount(
                        r.getCategory() == null ? "UNKNOWN" : r.getCategory().name(),
                        r.getCnt() == null ? 0 : r.getCnt()))
                .toList();

        var resolutions = feedbackRepository.findResolutionsBetween(start, end);
        double avgHours = 0.0;
        if (!resolutions.isEmpty()) {
            long totalMinutes = 0;
            int counted = 0;
            for (var r : resolutions) {
                if (r.getCreatedAt() == null || r.getHandledAt() == null) {
                    continue;
                }
                totalMinutes += Duration.between(r.getCreatedAt(), r.getHandledAt()).toMinutes();
                counted++;
            }
            if (counted > 0) {
                avgHours = totalMinutes / 60.0 / counted;
            }
        }
        return new FeedbackAnalyticsResult(safeFrom, safeTo, open, resolved, inReview + closed,
                avgHours, byCategory);
    }

    // Guard against unbounded per-day iteration and memory growth on the
    // caller-supplied range. 366 days covers a full year plus leap-day buffer,
    // which matches the analytics UI's max period.
    private static final long MAX_RANGE_DAYS = 366;

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be on or before to");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException(
                    "Analytics range must not exceed " + MAX_RANGE_DAYS + " days");
        }
    }
}
