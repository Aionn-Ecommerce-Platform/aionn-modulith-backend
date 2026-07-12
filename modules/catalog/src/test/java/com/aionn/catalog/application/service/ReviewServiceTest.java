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
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.mapper.ReviewResultMapper;
import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.application.port.out.review.ProductReviewPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Merchant;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductReview;
import com.aionn.catalog.domain.valueobject.ReviewStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final String REVIEW_ID = "01HZREV0000000000000000001";
    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
    private static final String MERCHANT_ID = "01HZMER0000000000000000001";
    private static final String USER_ID = "user-1";
    private static final String OWNER_ID = "owner-1";
    private static final String ADMIN_ID = "admin-1";
    private static final String ORDER_ID = "order-1";

    @Mock
    private ProductReviewPersistencePort reviewRepository;
    @Mock
    private ProductPersistencePort productRepository;
    @Mock
    private MerchantPersistencePort merchantRepository;
    @Mock
    private OrderQueryPort orderQueryPort;
    @Mock
    private ReviewResultMapper reviewResultMapper;
    @Mock
    private EventPublisher eventPublisher;

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

    @InjectMocks
    private ReviewService reviewService;

    private ReviewResult sampleResult;

    @BeforeEach
    void setUp() {
        sampleResult = new ReviewResult(REVIEW_ID, PRODUCT_ID, USER_ID, ORDER_ID,
                5, "t", "c", List.of(), "VISIBLE",
                null, null, null, null, null, null, null);
    }

    private Product productWithSku(String skuId) {
        Product p = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        p.categorize(List.of("cat-1"));
        p.defineVariant(skuId, Map.of("color", "red"), Money.of(new BigDecimal("10"), "VND"));
        p.pullEvents();
        return p;
    }

    private ProductReview makeReview() {
        ProductReview review = ProductReview.create(REVIEW_ID, PRODUCT_ID, USER_ID, ORDER_ID,
                5, "t", "c", List.of());
        review.pullEvents();
        return review;
    }

    @Test
    void submitRejectsWhenUserAlreadyReviewed() {
        when(reviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.submit(new SubmitReviewCommand(
                USER_ID, PRODUCT_ID, 5, "t", "c", List.of())))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.REVIEW_ALREADY_EXISTS.getCode());
    }

    @Test
    void submitRejectsWhenProductNotFound() {
        when(reviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.submit(new SubmitReviewCommand(
                USER_ID, PRODUCT_ID, 5, "t", "c", List.of())))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_NOT_FOUND.getCode());
    }

    @Test
    void submitRejectsWhenUserNotPurchased() {
        when(reviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productWithSku("sku-1")));
        when(orderQueryPort.findCompletedOrderIdForSkus(USER_ID, List.of("sku-1"))).thenReturn(null);

        assertThatThrownBy(() -> reviewService.submit(new SubmitReviewCommand(
                USER_ID, PRODUCT_ID, 5, "t", "c", List.of())))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.REVIEW_NOT_PURCHASED.getCode());
    }

    @Test
    void submitPersistsWhenEligible() {
        when(reviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productWithSku("sku-1")));
        when(orderQueryPort.findCompletedOrderIdForSkus(USER_ID, List.of("sku-1"))).thenReturn(ORDER_ID);
        when(reviewRepository.save(any(ProductReview.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewResultMapper.toResult(any(ProductReview.class))).thenReturn(sampleResult);

        reviewService.submit(new SubmitReviewCommand(USER_ID, PRODUCT_ID, 5, "t", "c", List.of()));

        verify(reviewRepository).save(any(ProductReview.class));
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void updateRejectsWhenNotOwner() {
        ProductReview review = makeReview();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.update(new UpdateReviewCommand(
                "other-user", REVIEW_ID, 4, "t2", "c2", List.of())))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.REVIEW_FORBIDDEN.getCode());
    }

    @Test
    void deleteMarksDeletedWhenOwner() {
        ProductReview review = makeReview();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        reviewService.delete(new DeleteReviewCommand(USER_ID, REVIEW_ID));

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.DELETED);
        verify(reviewRepository).save(review);
    }

    @Test
    void merchantReplyRequiresOwnership() {
        ProductReview review = makeReview();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        Product product = productWithSku("sku-1");
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(merchantRepository.findByOwnerId("stranger")).thenReturn(Optional.of(
                Merchant.register("other-mer", "stranger", "X", new BigDecimal("0.05"))));

        assertThatThrownBy(() -> reviewService.merchantReply(
                new MerchantReplyCommand("stranger", REVIEW_ID, "hi")))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.REVIEW_FORBIDDEN.getCode());
    }

    @Test
    void reportSetsReviewStatusReported() {
        ProductReview review = makeReview();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        Product product = productWithSku("sku-1");
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        Merchant merchant = Merchant.register(MERCHANT_ID, OWNER_ID, "Acme", new BigDecimal("0.05"));
        when(merchantRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(merchant));
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewResultMapper.toResult(review)).thenReturn(sampleResult);

        reviewService.report(new ReportReviewCommand(OWNER_ID, REVIEW_ID, "abuse"));

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.REPORTED);
    }

    @Test
    void hideChangesStatusToHidden() {
        ProductReview review = makeReview();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewResultMapper.toResult(review)).thenReturn(sampleResult);

        reviewService.hide(new HideReviewCommand(ADMIN_ID, REVIEW_ID));

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
    }

    @Test
    void adminDeleteSoftDeletes() {
        ProductReview review = makeReview();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        reviewService.adminDelete(new AdminDeleteReviewCommand(ADMIN_ID, REVIEW_ID));

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.DELETED);
        verify(reviewRepository).save(review);
    }

    @Test
    void restoreRequiresReviewInReportedStatus() {
        ProductReview review = makeReview();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.restore(new RestoreReviewCommand(ADMIN_ID, REVIEW_ID)))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.REVIEW_NOT_REPORTED.getCode());
    }

    @Test
    void restoreClearsReportedFields() {
        ProductReview review = makeReview();
        review.report(MERCHANT_ID, "abuse");
        review.pullEvents();
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewResultMapper.toResult(review)).thenReturn(sampleResult);

        reviewService.restore(new RestoreReviewCommand(ADMIN_ID, REVIEW_ID));

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.VISIBLE);
    }

    @Test
    void getByProductReturnsPage() {
        ProductReview review = makeReview();
        when(reviewRepository.findByProductIdAndStatus(PRODUCT_ID, ReviewStatus.VISIBLE, OffsetPagination.of(0, 20)))
                .thenReturn(List.of(review));
        when(reviewRepository.countByProductIdAndStatus(PRODUCT_ID, ReviewStatus.VISIBLE)).thenReturn(1L);
        when(reviewResultMapper.toResult(review)).thenReturn(sampleResult);

        PageResult<ReviewResult> page = reviewService.getByProduct(PRODUCT_ID, OffsetPagination.of(0, 20));

        assertThat(page.content()).containsExactly(sampleResult);
        assertThat(page.totalElements()).isEqualTo(1L);
    }

    @Test
    void getMyReviewsReturnsPage() {
        ProductReview review = makeReview();
        when(reviewRepository.findByUserId(USER_ID, OffsetPagination.of(0, 20)))
                .thenReturn(List.of(review));
        when(reviewRepository.countByUserId(USER_ID)).thenReturn(1L);
        when(reviewResultMapper.toResult(review)).thenReturn(sampleResult);

        PageResult<ReviewResult> page = reviewService.getMyReviews(USER_ID, OffsetPagination.of(0, 20));

        assertThat(page.content()).containsExactly(sampleResult);
    }

    @Test
    void getReportedReturnsPage() {
        ProductReview review = makeReview();
        review.report(MERCHANT_ID, "abuse");
        review.pullEvents();
        when(reviewRepository.findByStatus(ReviewStatus.REPORTED, OffsetPagination.of(0, 20)))
                .thenReturn(List.of(review));
        when(reviewRepository.countByStatus(ReviewStatus.REPORTED)).thenReturn(1L);
        when(reviewResultMapper.toResult(review)).thenReturn(sampleResult);

        PageResult<ReviewResult> page = reviewService.getReportedReviews(OffsetPagination.of(0, 20));

        assertThat(page.content()).containsExactly(sampleResult);
    }

    @Test
    void ratingSummaryReturnsAggregates() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productWithSku("sku-1")));
        when(reviewRepository.getAverageRating(PRODUCT_ID)).thenReturn(4.5);
        when(reviewRepository.countByProductIdAndStatus(PRODUCT_ID, ReviewStatus.VISIBLE)).thenReturn(2L);
        when(reviewRepository.getRatingDistribution(PRODUCT_ID))
                .thenReturn(Map.of(5, 1L, 4, 1L, 3, 0L, 2, 0L, 1, 0L));

        RatingSummary summary = reviewService.getProductRatingSummary(PRODUCT_ID);

        assertThat(summary.average()).isEqualTo(4.5);
        assertThat(summary.total()).isEqualTo(2L);
        assertThat(summary.distribution()).containsEntry(5, 1L);
    }

    @Test
    void checkEligibilityReturnsAlreadyReviewed() {
        when(reviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(true);

        ReviewEligibilityResult r = reviewService.checkEligibility(USER_ID, PRODUCT_ID);

        assertThat(r.canReview()).isFalse();
        assertThat(r.reason()).isEqualTo("ALREADY_REVIEWED");
    }

    @Test
    void checkEligibilityReturnsNotPurchased() {
        when(reviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productWithSku("sku-1")));
        when(orderQueryPort.findCompletedOrderIdForSkus(USER_ID, List.of("sku-1"))).thenReturn(null);

        ReviewEligibilityResult r = reviewService.checkEligibility(USER_ID, PRODUCT_ID);

        assertThat(r.canReview()).isFalse();
        assertThat(r.reason()).isEqualTo("NOT_PURCHASED");
    }

    @Test
    void checkEligibilityReturnsTrueWhenPurchased() {
        when(reviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productWithSku("sku-1")));
        when(orderQueryPort.findCompletedOrderIdForSkus(USER_ID, List.of("sku-1"))).thenReturn(ORDER_ID);

        ReviewEligibilityResult r = reviewService.checkEligibility(USER_ID, PRODUCT_ID);

        assertThat(r.canReview()).isTrue();
        assertThat(r.reason()).isNull();
    }

    @Test
    void reviewNotFoundThrowsCorrectError() {
        when(reviewRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.hide(new HideReviewCommand(ADMIN_ID, "missing")))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.REVIEW_NOT_FOUND.getCode());
    }
}
