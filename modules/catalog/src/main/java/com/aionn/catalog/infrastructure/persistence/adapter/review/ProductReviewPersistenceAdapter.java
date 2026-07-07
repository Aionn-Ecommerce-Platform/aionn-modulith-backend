package com.aionn.catalog.infrastructure.persistence.adapter.review;

import com.aionn.catalog.application.port.out.review.ProductReviewPersistencePort;
import com.aionn.catalog.domain.model.ProductReview;
import com.aionn.catalog.domain.valueobject.ReviewStatus;
import com.aionn.catalog.infrastructure.persistence.mapper.ReviewDomainMapper;
import com.aionn.catalog.infrastructure.persistence.repository.review.ProductReviewRepository;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductReviewPersistenceAdapter implements ProductReviewPersistencePort {

    private final ProductReviewRepository jpa;
    private final ReviewDomainMapper mapper;

    @Override
    public ProductReview save(ProductReview review) {
        return mapper.toDomain(jpa.save(mapper.toEntity(review)));
    }

    @Override
    public Optional<ProductReview> findById(String reviewId) {
        return jpa.findById(reviewId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUserIdAndProductId(String userId, String productId) {
        return jpa.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    public List<ProductReview> findByProductIdAndStatus(String productId, ReviewStatus status,
            OffsetPagination pagination) {
        return jpa.findByProductIdAndStatus(productId, status.name(),
                PageRequest.of(pagination.page(), pagination.size(),
                        Sort.by("createdAt").descending().and(Sort.by("reviewId").ascending())))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByProductIdAndStatus(String productId, ReviewStatus status) {
        return jpa.countByProductIdAndStatus(productId, status.name());
    }

    @Override
    public List<ProductReview> findByUserId(String userId, OffsetPagination pagination) {
        return jpa.findByUserId(userId,
                PageRequest.of(pagination.page(), pagination.size(),
                        Sort.by("createdAt").descending().and(Sort.by("reviewId").ascending())))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByUserId(String userId) {
        return jpa.countByUserId(userId);
    }

    @Override
    public List<ProductReview> findByStatus(ReviewStatus status, OffsetPagination pagination) {
        return jpa.findByStatus(status.name(),
                PageRequest.of(pagination.page(), pagination.size(),
                        Sort.by("reportedAt").descending().and(Sort.by("reviewId").ascending())))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByStatus(ReviewStatus status) {
        return jpa.countByStatus(status.name());
    }

    @Override
    public double getAverageRating(String productId) {
        return jpa.getAverageRating(productId);
    }

    @Override
    public Map<Integer, Long> getRatingDistribution(String productId) {
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }
        for (Object[] row : jpa.countRatingsGroupByRating(productId)) {
            distribution.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        return distribution;
    }
}
