package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.domain.model.ProductReview;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReviewResultMapper {

    public ReviewResult toResult(ProductReview review) {
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
                review.getUpdatedAt());
    }

    public List<ReviewResult> toResults(List<ProductReview> reviews) {
        if (reviews == null) {
            return List.of();
        }
        return reviews.stream()
                .map(this::toResult)
                .toList();
    }

    public PageResult<ReviewResult> toPageResult(PageResult<ProductReview> page) {
        return new PageResult<>(
                toResults(page.content()),
                page.page(),
                page.size(),
                page.totalElements());
    }
}
