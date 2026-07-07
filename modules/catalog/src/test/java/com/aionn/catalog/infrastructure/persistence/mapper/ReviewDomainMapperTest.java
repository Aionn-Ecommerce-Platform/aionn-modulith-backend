package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.ProductReview;
import com.aionn.catalog.domain.valueobject.ReviewStatus;
import com.aionn.catalog.infrastructure.persistence.entity.ProductReviewEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewDomainMapperTest {

    private final ReviewDomainMapper mapper = new ReviewDomainMapperImpl();

    private ProductReview sampleReview() {
        ProductReview review = ProductReview.create("r1", "p1", "u1", "o1", 5, "title", "content",
                List.of("img1.png"));
        review.pullEvents();
        return review;
    }

    @Test
    void toEntityMapsStatusName() {
        ProductReviewEntity entity = mapper.toEntity(sampleReview());

        assertThat(entity.getReviewId()).isEqualTo("r1");
        assertThat(entity.getRating()).isEqualTo(5);
        assertThat(entity.getStatus()).isEqualTo("VISIBLE");
    }

    @Test
    void roundTripBackToDomain() {
        ProductReview back = mapper.toDomain(mapper.toEntity(sampleReview()));

        assertThat(back.getReviewId()).isEqualTo("r1");
        assertThat(back.getProductId()).isEqualTo("p1");
        assertThat(back.getStatus()).isEqualTo(ReviewStatus.VISIBLE);
        assertThat(back.getImageUrls()).containsExactly("img1.png");
    }

    @Test
    void nullInputsReturnNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
