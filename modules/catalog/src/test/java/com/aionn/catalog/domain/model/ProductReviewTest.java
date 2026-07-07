package com.aionn.catalog.domain.model;

import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.valueobject.ReviewStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductReviewTest {

    private static final String REVIEW_ID = "01HZREV0000000000000000001";
    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
    private static final String USER_ID = "user-1";
    private static final String ORDER_ID = "order-1";
    private static final String MERCHANT_ID = "merchant-1";
    private static final String ADMIN_ID = "admin-1";

    private ProductReview freshReview() {
        ProductReview review = ProductReview.create(REVIEW_ID, PRODUCT_ID, USER_ID, ORDER_ID,
                5, "title", "content", List.of());
        review.pullEvents();
        return review;
    }

    @Test
    void createInitializesVisibleAndEmitsEvent() {
        ProductReview review = ProductReview.create(REVIEW_ID, PRODUCT_ID, USER_ID, ORDER_ID,
                4, "t", "c", List.of());

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.VISIBLE);
        assertThat(review.pullEvents()).hasSize(1);
    }

    @Test
    void createRejectsInvalidRating() {
        assertThatThrownBy(() -> ProductReview.create(REVIEW_ID, PRODUCT_ID, USER_ID, ORDER_ID,
                0, "t", "c", List.of()))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.REVIEW_INVALID_RATING.getCode());
    }

    @Test
    void createRejectsTooManyImages() {
        assertThatThrownBy(() -> ProductReview.create(REVIEW_ID, PRODUCT_ID, USER_ID, ORDER_ID,
                5, "t", "c", List.of("a", "b", "c", "d", "e", "f")))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void updateChangesFieldsAndEmitsEvent() {
        ProductReview review = freshReview();

        review.update(3, "new title", "new content", List.of("img1"));

        assertThat(review.getRating()).isEqualTo(3);
        assertThat(review.getTitle()).isEqualTo("new title");
        assertThat(review.getImageUrls()).containsExactly("img1");
        assertThat(review.pullEvents()).hasSize(1);
    }

    @Test
    void updateThrowsWhenHidden() {
        ProductReview review = freshReview();
        review.hide();
        review.pullEvents();

        assertThatThrownBy(() -> review.update(3, "t", "c", List.of()))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.REVIEW_FORBIDDEN.getCode());
    }

    @Test
    void replySetsReplyContentAndEvent() {
        ProductReview review = freshReview();

        review.reply("thanks!");

        assertThat(review.getMerchantReply()).isEqualTo("thanks!");
        assertThat(review.getMerchantRepliedAt()).isNotNull();
        assertThat(review.pullEvents()).hasSize(1);
    }

    @Test
    void hideChangesStatus() {
        ProductReview review = freshReview();
        review.hide();
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        assertThat(review.pullEvents()).hasSize(1);
    }

    @Test
    void reportSetsStatusAndMetadata() {
        ProductReview review = freshReview();

        review.report(MERCHANT_ID, "abusive language");

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.REPORTED);
        assertThat(review.getReportedByMerchantId()).isEqualTo(MERCHANT_ID);
        assertThat(review.getReportReason()).isEqualTo("abusive language");
        assertThat(review.getReportedAt()).isNotNull();
        assertThat(review.pullEvents()).hasSize(1);
    }

    @Test
    void reportThrowsWhenAlreadyReported() {
        ProductReview review = freshReview();
        review.report(MERCHANT_ID, "reason1");

        assertThatThrownBy(() -> review.report(MERCHANT_ID, "reason2"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.REVIEW_ALREADY_REPORTED.getCode());
    }

    @Test
    void reportThrowsWhenDeleted() {
        ProductReview review = freshReview();
        review.adminDelete(ADMIN_ID);

        assertThatThrownBy(() -> review.report(MERCHANT_ID, "any"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.REVIEW_FORBIDDEN.getCode());
    }

    @Test
    void adminDeleteChangesStatus() {
        ProductReview review = freshReview();
        review.adminDelete(ADMIN_ID);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.DELETED);
        assertThat(review.pullEvents()).hasSize(1);
    }

    @Test
    void restoreOnlyFromReported() {
        ProductReview review = freshReview();
        review.report(MERCHANT_ID, "reason");
        review.pullEvents();

        review.restore(ADMIN_ID);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.VISIBLE);
        assertThat(review.getReportedByMerchantId()).isNull();
        assertThat(review.getReportReason()).isNull();
        assertThat(review.getReportedAt()).isNull();
    }

    @Test
    void restoreThrowsWhenNotReported() {
        ProductReview review = freshReview();

        assertThatThrownBy(() -> review.restore(ADMIN_ID))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.REVIEW_NOT_REPORTED.getCode());
    }

    @Test
    void replyThrowsWhenDeleted() {
        ProductReview review = freshReview();
        review.adminDelete(ADMIN_ID);

        assertThatThrownBy(() -> review.reply("hi"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.REVIEW_FORBIDDEN.getCode());
    }
}
