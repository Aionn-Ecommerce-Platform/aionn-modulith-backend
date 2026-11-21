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
import com.aionn.contracts.port.ordering.OrderQueryPort;
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

    private final ProductReviewPersistencePort productReviewPersistencePort;
    private final ProductPersistencePort productPersistencePort;
    private final MerchantPersistencePort merchantPersistencePort;
    private final OrderQueryPort orderQueryPort;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public ProductReview submit(SubmitReviewCommand command) {
        Product product = productPersistencePort.findById(command.productId())
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
        List<String> skuIds = product.variants().stream().map(ProductVariant::skuId).toList();
        boolean hasOrderId = command.orderId() != null && !command.orderId().isBlank();
        boolean hasSkuId = command.skuId() != null && !command.skuId().isBlank();
        if (hasOrderId != hasSkuId) {
            throw new CatalogException(CatalogErrorCode.INVALID_ARGUMENT,
                    "orderId and skuId must be provided together");
        }
        OrderQueryPort.ReviewablePurchase purchase;
        if (hasOrderId) {
            if (!skuIds.contains(command.skuId())) {
                throw new CatalogException(CatalogErrorCode.REVIEW_NOT_PURCHASED);
            }
            purchase = orderQueryPort.findReviewablePurchase(
                            command.userId(), command.orderId(), command.skuId())
                    .orElseThrow(() -> new CatalogException(CatalogErrorCode.REVIEW_NOT_PURCHASED));
        } else {
            purchase = orderQueryPort.findLatestReviewablePurchase(command.userId(), skuIds)
                    .orElseThrow(() -> new CatalogException(CatalogErrorCode.REVIEW_NOT_PURCHASED));
        }
        if (productReviewPersistencePort.existsByUserIdAndOrderIdAndSkuId(
                command.userId(), purchase.orderId(), purchase.skuId())) {
            throw new CatalogException(CatalogErrorCode.REVIEW_ALREADY_EXISTS);
        }
        ProductReview review = ProductReview.create(
                new ProductReview.ReviewDraft(
                        IdGenerator.ulid(), command.productId(), command.userId(),
                        purchase.orderId(), purchase.skuId(),
                        command.rating(), command.title(), command.content(), command.imageUrls()),
                clock);
        ProductReview saved = productReviewPersistencePort.save(review);
        eventPublisher.publish(review.pullEvents());
        return saved;
    }

    public ProductReview update(UpdateReviewCommand command) {
        ProductReview review = required(command.reviewId());
        if (!review.getUserId().equals(command.userId())) {
            throw new CatalogException(CatalogErrorCode.REVIEW_FORBIDDEN, "User does not own this review");
        }
        review.update(command.rating(), command.title(), command.content(), command.imageUrls(), clock);
        ProductReview saved = productReviewPersistencePort.save(review);
        eventPublisher.publish(review.pullEvents());
        return saved;
    }

    public void delete(DeleteReviewCommand command) {
        ProductReview review = required(command.reviewId());
        if (!review.getUserId().equals(command.userId())) {
            throw new CatalogException(CatalogErrorCode.REVIEW_FORBIDDEN, "User does not own this review");
        }
        review.withdraw(command.userId(), clock);
        productReviewPersistencePort.save(review);
        eventPublisher.publish(review.pullEvents());
    }

    public ProductReview merchantReply(MerchantReplyCommand command) {
        ProductReview review = required(command.reviewId());
        ensureMerchantOwnsProduct(command.ownerId(), review.getProductId());
        review.reply(command.content(), clock);
        ProductReview saved = productReviewPersistencePort.save(review);
        eventPublisher.publish(review.pullEvents());
        return saved;
    }

    public ProductReview report(ReportReviewCommand command) {
        ProductReview review = required(command.reviewId());
        Merchant merchant = ensureMerchantOwnsProduct(command.ownerId(), review.getProductId());
        review.report(merchant.getMerchantId(), command.reason(), clock);
        ProductReview saved = productReviewPersistencePort.save(review);
        eventPublisher.publish(review.pullEvents());
        return saved;
    }

    public ProductReview hide(HideReviewCommand command) {
        ProductReview review = required(command.reviewId());
        review.hide(clock);
        ProductReview saved = productReviewPersistencePort.save(review);
        eventPublisher.publish(review.pullEvents());
        return saved;
    }

    public void adminDelete(AdminDeleteReviewCommand command) {
        ProductReview review = required(command.reviewId());
        review.adminDelete(command.adminId(), clock);
        productReviewPersistencePort.save(review);
        eventPublisher.publish(review.pullEvents());
    }

    public ProductReview restore(RestoreReviewCommand command) {
        ProductReview review = required(command.reviewId());
        review.restore(command.adminId(), clock);
        ProductReview saved = productReviewPersistencePort.save(review);
        eventPublisher.publish(review.pullEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public PageResult<ProductReview> getByProduct(String productId, OffsetPagination pagination) {
        List<ProductReview> content = productReviewPersistencePort
                .findByProductIdAndStatus(productId, ReviewStatus.VISIBLE, pagination);
        long total = productReviewPersistencePort.countByProductIdAndStatus(productId, ReviewStatus.VISIBLE);
        return new PageResult<>(content, pagination.page(), pagination.size(), total);
    }

    @Transactional(readOnly = true)
    public PageResult<ProductReview> getMyReviews(String userId, OffsetPagination pagination) {
        List<ProductReview> content = productReviewPersistencePort.findByUserId(userId, pagination);
        long total = productReviewPersistencePort.countByUserId(userId);
        return new PageResult<>(content, pagination.page(), pagination.size(), total);
    }

    @Transactional(readOnly = true)
    public PageResult<ProductReview> getAdminProductReviews(String productId, OffsetPagination pagination) {
        productPersistencePort.findById(productId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
        List<ProductReview> content = productReviewPersistencePort.findByProductId(productId, pagination);
        return new PageResult<>(content, pagination.page(), pagination.size(),
                productReviewPersistencePort.countByProductId(productId));
    }

    @Transactional(readOnly = true)
    public PageResult<ProductReview> getMerchantReviews(
            String ownerId, Boolean replied, OffsetPagination pagination) {
        Merchant merchant = merchantPersistencePort.findByOwnerId(ownerId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.MERCHANT_NOT_FOUND));
        List<ProductReview> content = productReviewPersistencePort.findByMerchantId(
                merchant.getMerchantId(), replied, pagination);
        long total = productReviewPersistencePort.countByMerchantId(merchant.getMerchantId(), replied);
        return new PageResult<>(content, pagination.page(), pagination.size(), total);
    }

    @Transactional(readOnly = true)
    public PageResult<ProductReview> getReportedReviews(OffsetPagination pagination) {
        List<ProductReview> content = productReviewPersistencePort.findByStatus(ReviewStatus.REPORTED, pagination);
        long total = productReviewPersistencePort.countByStatus(ReviewStatus.REPORTED);
        return new PageResult<>(content, pagination.page(), pagination.size(), total);
    }

    @Transactional(readOnly = true)
    public RatingSummary getProductRatingSummary(String productId) {
        productPersistencePort.findById(productId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
        double avg = productReviewPersistencePort.getAverageRating(productId);
        long total = productReviewPersistencePort.countByProductIdAndStatus(productId, ReviewStatus.VISIBLE);
        return new RatingSummary(productId, avg, total, productReviewPersistencePort.getRatingDistribution(productId));
    }

    @Transactional(readOnly = true)
    public ReviewEligibilityResult checkEligibility(String userId, String productId,
            String orderId, String skuId) {
        Product product = productPersistencePort.findById(productId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
        List<String> skuIds = product.variants().stream().map(ProductVariant::skuId).toList();
        boolean hasOrderId = orderId != null && !orderId.isBlank();
        boolean hasSkuId = skuId != null && !skuId.isBlank();
        if (hasOrderId != hasSkuId) {
            throw new CatalogException(CatalogErrorCode.INVALID_ARGUMENT,
                    "orderId and skuId must be provided together");
        }
        OrderQueryPort.ReviewablePurchase purchase;
        if (hasOrderId) {
            if (!skuIds.contains(skuId)) {
                return new ReviewEligibilityResult(false, "NOT_PURCHASED", null, null);
            }
            purchase = orderQueryPort.findReviewablePurchase(userId, orderId, skuId).orElse(null);
        } else {
            purchase = orderQueryPort.findLatestReviewablePurchase(userId, skuIds).orElse(null);
        }
        if (purchase == null) {
            return new ReviewEligibilityResult(false, "NOT_PURCHASED", null, null);
        }
        if (productReviewPersistencePort.existsByUserIdAndOrderIdAndSkuId(
                userId, purchase.orderId(), purchase.skuId())) {
            return new ReviewEligibilityResult(
                    false, "ALREADY_REVIEWED", purchase.orderId(), purchase.skuId());
        }
        return new ReviewEligibilityResult(
                true, null, purchase.orderId(), purchase.skuId());
    }

    private ProductReview required(String reviewId) {
        return productReviewPersistencePort.findById(reviewId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.REVIEW_NOT_FOUND));
    }

    private Merchant ensureMerchantOwnsProduct(String ownerId, String productId) {
        Product product = productPersistencePort.findById(productId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
        Merchant merchant = merchantPersistencePort.findByOwnerId(ownerId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.MERCHANT_NOT_FOUND));
        if (!product.getMerchantId().equals(merchant.getMerchantId())) {
            throw new CatalogException(CatalogErrorCode.REVIEW_FORBIDDEN,
                    "Merchant does not own the reviewed product");
        }
        return merchant;
    }
}
