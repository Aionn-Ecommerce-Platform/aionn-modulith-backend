package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.application.port.out.product.ProductSoldCounterPersistencePort;
import com.aionn.catalog.application.port.out.review.ProductReviewPersistencePort;
import com.aionn.catalog.domain.model.Product;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSearchDocumentMapperTest {

    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
    private static final String MERCHANT_ID = "01HZMER0000000000000000001";

    @Mock
    private ProductReviewPersistencePort reviewRepository;
    @Mock
    private ProductSoldCounterPersistencePort soldCounterRepository;

    @InjectMocks
    private ProductSearchDocumentMapper mapper;

    @Test
    void buildsDocumentWithPriceRangeRatingAndSoldCount() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.categorize(java.util.List.of("c1"));
        product.defineVariant("sku-1", Map.of("color", "red"), Money.of(new BigDecimal("100"), "VND"));
        product.defineVariant("sku-2", Map.of("color", "blue"), Money.of(new BigDecimal("150"), "VND"));
        when(reviewRepository.getAverageRating(PRODUCT_ID)).thenReturn(4.2);
        when(soldCounterRepository.getSoldCount(PRODUCT_ID)).thenReturn(7L);

        ProductSearchDocument doc = mapper.toSearchDocument(product, Map.of("color", "red"));

        assertThat(doc.productId()).isEqualTo(PRODUCT_ID);
        assertThat(doc.priceFrom()).isEqualByComparingTo("100");
        assertThat(doc.priceTo()).isEqualByComparingTo("150");
        assertThat(doc.currency()).isEqualTo("VND");
        assertThat(doc.rating()).isEqualTo(4.2);
        assertThat(doc.soldCount()).isEqualTo(7L);
        assertThat(doc.filterableAttributes()).containsEntry("color", "red");
    }

    @Test
    void toleratesProductWithoutVariants() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        when(reviewRepository.getAverageRating(PRODUCT_ID)).thenReturn(0.0);
        when(soldCounterRepository.getSoldCount(PRODUCT_ID)).thenReturn(0L);

        ProductSearchDocument doc = mapper.toSearchDocument(product, null);

        assertThat(doc.priceFrom()).isNull();
        assertThat(doc.filterableAttributes()).isEmpty();
    }
}
