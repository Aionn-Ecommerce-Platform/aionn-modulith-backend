package com.aionn.identity.infrastructure.persistence.adapter.analytics;

import com.aionn.identity.application.dto.analytics.result.FeedbackAnalyticsResult;
import com.aionn.identity.application.dto.analytics.result.KycAnalyticsResult;
import com.aionn.identity.application.dto.analytics.result.UserAnalyticsResult;
import com.aionn.identity.domain.valueobject.FeedbackCategory;
import com.aionn.identity.domain.valueobject.FeedbackStatus;
import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.identity.infrastructure.persistence.repository.feedback.UserFeedbackRepository;
import com.aionn.identity.infrastructure.persistence.repository.kyc.KycProfileRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityAnalyticsAdapterTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private KycProfileRepository kycRepository;
    @Mock
    private UserFeedbackRepository feedbackRepository;

    @InjectMocks
    private IdentityAnalyticsAdapter adapter;

    private UserRepository.StatusCount userStatusCount(UserStatus status, Long cnt) {
        UserRepository.StatusCount row = mock(UserRepository.StatusCount.class);
        when(row.getStatus()).thenReturn(status);
        when(row.getCnt()).thenReturn(cnt);
        return row;
    }

    private UserRepository.RoleCount userRoleCount(UserRole role, Long cnt) {
        UserRepository.RoleCount row = mock(UserRepository.RoleCount.class);
        when(row.getRole()).thenReturn(role);
        when(row.getCnt()).thenReturn(cnt);
        return row;
    }

    @Test
    void getUserAnalyticsBuildsTrendAndBreakdowns() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 3);
        List<UserRepository.StatusCount> statusRows = List.of(userStatusCount(UserStatus.ACTIVE, 40L),
                userStatusCount(null, null));
        List<UserRepository.RoleCount> roleRows = List.of(userRoleCount(UserRole.BUYER, 30L),
                userRoleCount(null, null));
        when(userRepository.findCreatedAtBetween(any(), any()))
                .thenReturn(List.of(from.atTime(10, 0), to.atTime(9, 0)));
        when(userRepository.count()).thenReturn(42L);
        when(userRepository.countByStatus()).thenReturn(statusRows);
        when(userRepository.countByRole()).thenReturn(roleRows);

        UserAnalyticsResult result = adapter.getUserAnalytics(from, to);

        assertThat(result.from()).isEqualTo(from);
        assertThat(result.to()).isEqualTo(to);
        assertThat(result.totalUsers()).isEqualTo(42L);
        assertThat(result.newUsersInRange()).isEqualTo(2L);
        assertThat(result.signupTrend()).hasSize(3);
        assertThat(result.signupTrend().get(0).count()).isEqualTo(1L);
        assertThat(result.statusBreakdown())
                .anySatisfy(s -> assertThat(s.status()).isEqualTo("ACTIVE"))
                .anySatisfy(s -> assertThat(s.status()).isEqualTo("UNKNOWN"));
        assertThat(result.roleBreakdown())
                .anySatisfy(r -> assertThat(r.role()).isEqualTo("BUYER"))
                .anySatisfy(r -> assertThat(r.role()).isEqualTo("UNKNOWN"));
    }

    @Test
    void getUserAnalyticsUsesDefaultsWhenNullRange() {
        when(userRepository.findCreatedAtBetween(any(), any())).thenReturn(List.of());
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByStatus()).thenReturn(List.of());
        when(userRepository.countByRole()).thenReturn(List.of());

        UserAnalyticsResult result = adapter.getUserAnalytics(null, null);

        assertThat(result.to()).isEqualTo(result.from().plusDays(29));
        assertThat(result.signupTrend()).hasSize(30);
    }

    @Test
    void getUserAnalyticsRejectsInvertedRange() {
        assertThatThrownBy(() -> adapter.getUserAnalytics(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getUserAnalyticsRejectsTooLargeRange() {
        assertThatThrownBy(() -> adapter.getUserAnalytics(LocalDate.of(2020, 1, 1), LocalDate.of(2024, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getKycAnalyticsComputesRatesAndProcessingTime() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 10);
        List<KycProfileRepository.KycStatusCount> statusRows = List.of(
                kycStatusCount("PENDING", 2L),
                kycStatusCount("APPROVED", 6L),
                kycStatusCount("REJECTED", 2L),
                kycStatusCount("SUBMITTED", 1L),
                kycStatusCount("CANCELLED", 3L));
        KycProfileRepository.KycDecisionProjection decision = mock(KycProfileRepository.KycDecisionProjection.class);
        when(decision.getSubmittedAt()).thenReturn(from.atTime(10, 0));
        when(decision.getApprovedAt()).thenReturn(from.atTime(12, 0));
        when(kycRepository.countByStatus()).thenReturn(statusRows);
        when(kycRepository.findDecisionsBetween(any(), any())).thenReturn(List.of(decision));

        KycAnalyticsResult result = adapter.getKycAnalytics(from, to);

        assertThat(result.pending()).isEqualTo(2L);
        assertThat(result.approved()).isEqualTo(6L);
        assertThat(result.rejected()).isEqualTo(2L);
        assertThat(result.submitted()).isEqualTo(1L);
        assertThat(result.approvalRate()).isEqualTo(6.0 / 8.0);
        assertThat(result.avgProcessingHours()).isEqualTo(2.0);
    }

    @Test
    void getKycAnalyticsHandlesNoDecisions() {
        when(kycRepository.countByStatus()).thenReturn(List.of());
        when(kycRepository.findDecisionsBetween(any(), any())).thenReturn(List.of());

        KycAnalyticsResult result = adapter.getKycAnalytics(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2));

        assertThat(result.approvalRate()).isZero();
        assertThat(result.avgProcessingHours()).isZero();
    }

    @Test
    void getFeedbackAnalyticsComputesBreakdownAndResolutionTime() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 10);
        List<UserFeedbackRepository.FeedbackStatusCount> statusRows = List.of(
                feedbackStatusCount(FeedbackStatus.OPEN, 4L),
                feedbackStatusCount(FeedbackStatus.RESOLVED, 3L),
                feedbackStatusCount(FeedbackStatus.IN_REVIEW, 2L),
                feedbackStatusCount(FeedbackStatus.CLOSED, 1L));
        List<UserFeedbackRepository.FeedbackCategoryCount> categoryRows = List.of(
                feedbackCategoryCount(FeedbackCategory.BUG, 5L),
                feedbackCategoryCount(null, null));
        UserFeedbackRepository.FeedbackResolutionProjection resolution = mock(
                UserFeedbackRepository.FeedbackResolutionProjection.class);
        when(resolution.getCreatedAt()).thenReturn(from.atTime(8, 0));
        when(resolution.getHandledAt()).thenReturn(from.atTime(11, 0));
        when(feedbackRepository.countByStatus()).thenReturn(statusRows);
        when(feedbackRepository.countByCategory()).thenReturn(categoryRows);
        when(feedbackRepository.findResolutionsBetween(any(), any())).thenReturn(List.of(resolution));

        FeedbackAnalyticsResult result = adapter.getFeedbackAnalytics(from, to);

        assertThat(result.open()).isEqualTo(4L);
        assertThat(result.resolved()).isEqualTo(3L);
        assertThat(result.otherActive()).isEqualTo(3L);
        assertThat(result.avgResolutionHours()).isEqualTo(3.0);
        assertThat(result.byCategory())
                .anySatisfy(c -> assertThat(c.category()).isEqualTo("BUG"))
                .anySatisfy(c -> assertThat(c.category()).isEqualTo("UNKNOWN"));
    }

    private KycProfileRepository.KycStatusCount kycStatusCount(String status, Long cnt) {
        KycProfileRepository.KycStatusCount row = mock(KycProfileRepository.KycStatusCount.class);
        when(row.getStatus()).thenReturn(status);
        when(row.getCnt()).thenReturn(cnt);
        return row;
    }

    private UserFeedbackRepository.FeedbackStatusCount feedbackStatusCount(FeedbackStatus status, Long cnt) {
        UserFeedbackRepository.FeedbackStatusCount row = mock(UserFeedbackRepository.FeedbackStatusCount.class);
        when(row.getStatus()).thenReturn(status);
        when(row.getCnt()).thenReturn(cnt);
        return row;
    }

    private UserFeedbackRepository.FeedbackCategoryCount feedbackCategoryCount(FeedbackCategory category, Long cnt) {
        UserFeedbackRepository.FeedbackCategoryCount row = mock(UserFeedbackRepository.FeedbackCategoryCount.class);
        when(row.getCategory()).thenReturn(category);
        when(row.getCnt()).thenReturn(cnt);
        return row;
    }
}
