package com.aionn.catalog.infrastructure.persistence.adapter.review;

import com.aionn.catalog.application.port.out.review.ProductReviewPersistencePort;
import com.aionn.catalog.domain.model.ProductReview;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.valueobject.ReviewStatus;
import com.aionn.catalog.infrastructure.persistence.entity.ProductReviewEntity;
import com.aionn.catalog.infrastructure.persistence.mapper.ReviewDomainMapper;
import com.aionn.catalog.infrastructure.persistence.repository.review.ProductReviewRepository;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataIntegrityViolationException;

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
        try {
            return mapper.toDomain(jpa.save(mapper.toEntity(review)));
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateReviewViolation(ex)) {
                throw new CatalogException(CatalogErrorCode.REVIEW_ALREADY_EXISTS);
            }
            throw ex;
        }
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
                        Sort.by(Sort.Direction.DESC, TypedPropertyPath.path(ProductReviewEntity::getCreatedAt))
                                .and(Sort.by(Sort.Direction.ASC, TypedPropertyPath.path(ProductReviewEntity::getReviewId)))))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByProductIdAndStatus(String productId, ReviewStatus status) {
        return jpa.countByProductIdAndStatus(productId, status.name());
    }

    @Override
    public List<ProductReview> findByProductId(String productId, OffsetPagination pagination) {
        return jpa.findByProductId(productId,
                PageRequest.of(pagination.page(), pagination.size(),
                        Sort.by(Sort.Direction.DESC, TypedPropertyPath.path(ProductReviewEntity::getCreatedAt))))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countByProductId(String productId) {
        return jpa.countByProductId(productId);
    }

    @Override
    public List<ProductReview> findByUserId(String userId, OffsetPagination pagination) {
        return jpa.findByUserId(userId,
                PageRequest.of(pagination.page(), pagination.size(),
                        Sort.by(Sort.Direction.DESC, TypedPropertyPath.path(ProductReviewEntity::getCreatedAt))
                                .and(Sort.by(Sort.Direction.ASC, TypedPropertyPath.path(ProductReviewEntity::getReviewId)))))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByUserId(String userId) {
        return jpa.countByUserId(userId);
    }

    @Override
    public List<ProductReview> findByMerchantId(String merchantId, Boolean replied, OffsetPagination pagination) {
        return jpa.findByMerchantId(merchantId, replied,
                PageRequest.of(pagination.page(), pagination.size(),
                        Sort.by(Sort.Direction.DESC, TypedPropertyPath.path(ProductReviewEntity::getCreatedAt))
                                .and(Sort.by(Sort.Direction.ASC,
                                        TypedPropertyPath.path(ProductReviewEntity::getReviewId)))))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByMerchantId(String merchantId, Boolean replied) {
        return jpa.countByMerchantId(merchantId, replied);
    }

    @Override
    public List<ProductReview> findByStatus(ReviewStatus status, OffsetPagination pagination) {
        return jpa.findByStatus(status.name(),
                PageRequest.of(pagination.page(), pagination.size(),
                        Sort.by(Sort.Direction.DESC, TypedPropertyPath.path(ProductReviewEntity::getReportedAt))
                                .and(Sort.by(Sort.Direction.ASC, TypedPropertyPath.path(ProductReviewEntity::getReviewId)))))
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

    private static boolean isDuplicateReviewViolation(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(java.util.Locale.ROOT);
                if (normalized.contains("uq_reviews_user_product")
                        || (normalized.contains("user_id") && normalized.contains("product_id"))) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
