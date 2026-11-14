package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.domain.model.ProductReview;
import com.aionn.contracts.port.identity.UserProfileLookupPort;
import com.aionn.contracts.port.identity.UserProfileLookupPort.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReviewResultMapper {

    private final UserProfileLookupPort userProfileLookupPort;

    public ReviewResult toResult(ProductReview review) {
        return toResult(review, null);
    }

    public List<ReviewResult> toResults(List<ProductReview> reviews) {
        if (reviews == null) {
            return List.of();
        }
        Set<String> userIds = reviews.stream()
                .map(ProductReview::getUserId)
                .filter(Objects::nonNull)
                .filter(userId -> !userId.isBlank())
                .collect(Collectors.toSet());
        Map<String, UserProfile> profiles = userIds.isEmpty()
                ? Map.of()
                : userProfileLookupPort.findProfiles(userIds);
        return reviews.stream()
                .map(review -> toResult(review, profiles.get(review.getUserId())))
                .toList();
    }

    public PageResult<ReviewResult> toPageResult(PageResult<ProductReview> page) {
        return new PageResult<>(
                toResults(page.content()),
                page.page(),
                page.size(),
                page.totalElements());
    }

    private static ReviewResult toResult(ProductReview review, UserProfile profile) {
        if (review == null) {
            return null;
        }
        return new ReviewResult(
                review.getReviewId(),
                review.getProductId(),
                review.getUserId(),
                review.getOrderId(),
                review.getRating(),
                review.getTitle(),
                review.getContent(),
                List.copyOf(review.getImageUrls()),
                review.getStatus().name(),
                review.getMerchantReply(),
                review.getMerchantRepliedAt(),
                review.getReportedByMerchantId(),
                review.getReportReason(),
                review.getReportedAt(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                profile == null ? null : profile.username(),
                profile == null ? null : profile.displayName(),
                profile == null ? null : profile.avatarUrl());
    }
}
