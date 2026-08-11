package com.aionn.catalog.application.service;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.command.AdminDeleteReviewCommand;
import com.aionn.catalog.application.dto.review.command.DeleteReviewCommand;
import com.aionn.catalog.application.dto.review.command.HideReviewCommand;
import com.aionn.catalog.application.dto.review.command.MerchantReplyCommand;
import com.aionn.catalog.application.dto.review.command.ReportReviewCommand;
import com.aionn.catalog.application.dto.review.command.RestoreReviewCommand;
import com.aionn.catalog.application.dto.review.command.SubmitReviewCommand;
import com.aionn.catalog.application.dto.review.command.UpdateReviewCommand;
import com.aionn.catalog.application.dto.review.result.RatingSummary;
import com.aionn.catalog.application.dto.review.result.ReviewEligibilityResult;
import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.application.port.out.review.ProductReviewPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Merchant;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductReview;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.catalog.domain.valueobject.ReviewStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {

    private final ProductReviewPersistencePort reviewRepository;
    private final ProductPersistencePort productRepository;
    private final MerchantPersistencePort merchantRepository;
    private final OrderQueryPort orderQueryPort;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public ProductReview submit(SubmitReviewCommand command) {
        if (reviewRepository.existsByUserIdAndProductId(command.userId(), command.productId())) {
            throw new CatalogException(CatalogErrorCode.REVIEW_ALREADY_EXISTS);
        }
        Product product = productRepository.findById(command.productId())
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
        List<String> skuIds = product.variants().stream().map(ProductVariant::skuId).toList();
        String orderId = orderQueryPort.findCompletedOrderIdForSkus(command.userId(), skuIds);
        if (orderId == null) {
            throw new CatalogException(CatalogErrorCode.REVIEW_NOT_PURCHASED);
        }
        ProductReview review = ProductReview.create(
                new ProductReview.ReviewDraft(
                        IdGenerator.ulid(), command.productId(), command.userId(), orderId,
                        command.rating(), command.title(), command.content(), command.imageUrls()),
                clock);
        ProductReview saved = reviewRepository.save(review);
        eventPublisher.publish(review.pullEvents());
        return saved;
    }

    public ProductReview update(UpdateReviewCommand command) {
        ProductReview review = required(command.reviewId());
        if (!review.getUserId().equals(command.userId())) {
            throw new CatalogException(CatalogErrorCode.REVIEW_FORBIDDEN, "User does not own this review");
        }
        review.update(command.rating(), command.title(), command.content(), command.imageUrls(), clock);
        ProductReview saved = reviewRepository.save(review);
        eventPublisher.publish(review.pullEvents());
        return saved;
    }

    public void delete(DeleteReviewCommand command) {
        ProductReview review = required(command.reviewId());
        if (!review.getUserId().equals(command.userId())) {
            throw new CatalogException(CatalogErrorCode.REVIEW_FORBIDDEN, "User does not own this review");
        }
        review.withdraw(command.userId(), clock);
        reviewRepository.save(review);
        eventPublisher.publish(review.pullEvents());
    }

    public ProductReview merchantReply(MerchantReplyCommand command) {
        ProductReview review = required(command.reviewId());
        ensureMerchantOwnsProduct(command.ownerId(), review.getProductId());
        review.reply(command.content(), clock);
        ProductReview saved = reviewRepository.save(review);
        eventPublisher.publish(review.pullEvents());
        return saved;
    }

    public ProductReview report(ReportReviewCommand command) {
        ProductReview review = required(command.reviewId());
        Merchant merchant = ensureMerchantOwnsProduct(command.ownerId(), review.getProductId());
        review.report(merchant.getMerchantId(), command.reason(), clock);
        ProductReview saved = reviewRepository.save(review);
        eventPublisher.publish(review.pullEvents());
        return saved;
    }

    public ProductReview hide(HideReviewCommand command) {
        ProductReview review = required(command.reviewId());
        review.hide(clock);
        ProductReview saved = reviewRepository.save(review);
        eventPublisher.publish(review.pullEvents());
        return saved;
    }

    public void adminDelete(AdminDeleteReviewCommand command) {
        ProductReview review = required(command.reviewId());
        review.adminDelete(command.adminId(), clock);
        reviewRepository.save(review);
        eventPublisher.publish(review.pullEvents());
    }

    public ProductReview restore(RestoreReviewCommand command) {
        ProductReview review = required(command.reviewId());
        review.restore(command.adminId(), clock);
        ProductReview saved = reviewRepository.save(review);
        eventPublisher.publish(review.pullEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public PageResult<ProductReview> getByProduct(String productId, OffsetPagination pagination) {
        List<ProductReview> content = reviewRepository
                .findByProductIdAndStatus(productId, ReviewStatus.VISIBLE, pagination);
        long total = reviewRepository.countByProductIdAndStatus(productId, ReviewStatus.VISIBLE);
        return new PageResult<>(content, pagination.page(), pagination.size(), total);
    }

    @Transactional(readOnly = true)
    public PageResult<ProductReview> getMyReviews(String userId, OffsetPagination pagination) {
        List<ProductReview> content = reviewRepository.findByUserId(userId, pagination);
        long total = reviewRepository.countByUserId(userId);
        return new PageResult<>(content, pagination.page(), pagination.size(), total);
    }

    @Transactional(readOnly = true)
    public PageResult<ProductReview> getAdminProductReviews(String productId, OffsetPagination pagination) {
        productRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
        List<ProductReview> content = reviewRepository.findByProductId(productId, pagination);
        return new PageResult<>(content, pagination.page(), pagination.size(),
                reviewRepository.countByProductId(productId));
    }

    @Transactional(readOnly = true)
    public PageResult<ProductReview> getMerchantReviews(
            String ownerId, Boolean replied, OffsetPagination pagination) {
        Merchant merchant = merchantRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.MERCHANT_NOT_FOUND));
        List<ProductReview> content = reviewRepository.findByMerchantId(
                merchant.getMerchantId(), replied, pagination);
        long total = reviewRepository.countByMerchantId(merchant.getMerchantId(), replied);
        return new PageResult<>(content, pagination.page(), pagination.size(), total);
    }

    @Transactional(readOnly = true)
    public PageResult<ProductReview> getReportedReviews(OffsetPagination pagination) {
        List<ProductReview> content = reviewRepository.findByStatus(ReviewStatus.REPORTED, pagination);
        long total = reviewRepository.countByStatus(ReviewStatus.REPORTED);
        return new PageResult<>(content, pagination.page(), pagination.size(), total);
    }

    @Transactional(readOnly = true)
    public RatingSummary getProductRatingSummary(String productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
        double avg = reviewRepository.getAverageRating(productId);
        long total = reviewRepository.countByProductIdAndStatus(productId, ReviewStatus.VISIBLE);
        return new RatingSummary(productId, avg, total, reviewRepository.getRatingDistribution(productId));
    }

    @Transactional(readOnly = true)
    public ReviewEligibilityResult checkEligibility(String userId, String productId) {
        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            return new ReviewEligibilityResult(false, "ALREADY_REVIEWED");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
        List<String> skuIds = product.variants().stream().map(ProductVariant::skuId).toList();
        String orderId = orderQueryPort.findCompletedOrderIdForSkus(userId, skuIds);
        if (orderId == null) {
            return new ReviewEligibilityResult(false, "NOT_PURCHASED");
        }
        return new ReviewEligibilityResult(true, null);
    }

    private ProductReview required(String reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.REVIEW_NOT_FOUND));
    }

    private Merchant ensureMerchantOwnsProduct(String ownerId, String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
        Merchant merchant = merchantRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.MERCHANT_NOT_FOUND));
        if (!product.getMerchantId().equals(merchant.getMerchantId())) {
            throw new CatalogException(CatalogErrorCode.REVIEW_FORBIDDEN,
                    "Merchant does not own the reviewed product");
        }
        return merchant;
    }
}
