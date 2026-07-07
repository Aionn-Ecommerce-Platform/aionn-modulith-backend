package com.aionn.catalog.infrastructure.persistence.adapter.review;

import com.aionn.catalog.domain.model.ProductReview;
import com.aionn.catalog.domain.valueobject.ReviewStatus;
import com.aionn.catalog.infrastructure.persistence.entity.ProductReviewEntity;
import com.aionn.catalog.infrastructure.persistence.mapper.ReviewDomainMapper;
import com.aionn.catalog.infrastructure.persistence.repository.review.ProductReviewRepository;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReviewPersistenceAdapterTest {

    private static final String REVIEW_ID = "01HZREV0000000000000000001";
    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
    private static final String USER_ID = "user-1";

    @Mock
    private ProductReviewRepository jpa;
    @Mock
    private ReviewDomainMapper mapper;

    @InjectMocks
    private ProductReviewPersistenceAdapter adapter;

    private ProductReview domainReview() {
        return ProductReview.create(REVIEW_ID, PRODUCT_ID, USER_ID, "order-1",
                5, "t", "c", List.of());
    }

    @Test
    void saveMapsThroughEntityAndBack() {
        ProductReview domain = domainReview();
        ProductReviewEntity entity = new ProductReviewEntity();
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.save(domain)).isSameAs(domain);
    }

    @Test
    void findByIdReturnsMappedDomain() {
        ProductReviewEntity entity = new ProductReviewEntity();
        when(jpa.findById(REVIEW_ID)).thenReturn(Optional.of(entity));
        ProductReview review = domainReview();
        when(mapper.toDomain(entity)).thenReturn(review);

        assertThat(adapter.findById(REVIEW_ID)).contains(review);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(jpa.findById("missing")).thenReturn(Optional.empty());
        assertThat(adapter.findById("missing")).isEmpty();
    }

    @Test
    void existsByUserIdAndProductIdDelegates() {
        when(jpa.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(true);
        assertThat(adapter.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).isTrue();
    }

    @Test
    void findByProductIdAndStatusMapsResults() {
        ProductReviewEntity entity = new ProductReviewEntity();
        ProductReview review = domainReview();
        when(jpa.findByProductIdAndStatus(eq(PRODUCT_ID), eq("VISIBLE"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(review);

        List<ProductReview> reviews = adapter.findByProductIdAndStatus(PRODUCT_ID,
                ReviewStatus.VISIBLE, OffsetPagination.of(0, 10));

        assertThat(reviews).containsExactly(review);
    }

    @Test
    void countByProductIdAndStatusDelegates() {
        when(jpa.countByProductIdAndStatus(PRODUCT_ID, "VISIBLE")).thenReturn(3L);
        assertThat(adapter.countByProductIdAndStatus(PRODUCT_ID, ReviewStatus.VISIBLE)).isEqualTo(3L);
    }

    @Test
    void findByUserIdMapsResults() {
        ProductReviewEntity entity = new ProductReviewEntity();
        ProductReview review = domainReview();
        when(jpa.findByUserId(eq(USER_ID), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(review);

        assertThat(adapter.findByUserId(USER_ID, OffsetPagination.of(0, 10))).containsExactly(review);
    }

    @Test
    void countByUserIdDelegates() {
        when(jpa.countByUserId(USER_ID)).thenReturn(4L);
        assertThat(adapter.countByUserId(USER_ID)).isEqualTo(4L);
    }

    @Test
    void findByStatusMapsResults() {
        ProductReviewEntity entity = new ProductReviewEntity();
        ProductReview review = domainReview();
        when(jpa.findByStatus(eq("REPORTED"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(review);

        assertThat(adapter.findByStatus(ReviewStatus.REPORTED, OffsetPagination.of(0, 10)))
                .containsExactly(review);
    }

    @Test
    void countByStatusDelegates() {
        when(jpa.countByStatus("REPORTED")).thenReturn(5L);
        assertThat(adapter.countByStatus(ReviewStatus.REPORTED)).isEqualTo(5L);
    }

    @Test
    void getAverageRatingDelegates() {
        when(jpa.getAverageRating(PRODUCT_ID)).thenReturn(4.2);
        assertThat(adapter.getAverageRating(PRODUCT_ID)).isEqualTo(4.2);
    }

    @Test
    void getRatingDistributionBuildsFullMap() {
        when(jpa.countRatingsGroupByRating(PRODUCT_ID)).thenReturn(List.of(
                new Object[] { Integer.valueOf(5), Long.valueOf(3) },
                new Object[] { Integer.valueOf(4), Long.valueOf(2) }));

        Map<Integer, Long> distribution = adapter.getRatingDistribution(PRODUCT_ID);

        assertThat(distribution).hasSize(5);
        assertThat(distribution.get(5)).isEqualTo(3L);
        assertThat(distribution.get(4)).isEqualTo(2L);
        assertThat(distribution.get(3)).isZero();
        assertThat(distribution.get(2)).isZero();
        assertThat(distribution.get(1)).isZero();
    }
}
