package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.catalog.infrastructure.persistence.entity.ProductEntity;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDomainMapperTest {

    private final ProductDomainMapper mapper = new ProductDomainMapperImpl();

    private Product sampleProduct() {
        ProductVariant variant = new ProductVariant("sku-1", Map.of("color", "red"),
                Money.of(new BigDecimal("100"), "VND"), Money.of(new BigDecimal("120"), "VND"));
        Instant now = Instant.now();
        return new Product(
                "p1", "m1", "Widget", "b1",
                List.of("c1", "c2"), List.of("img1"), List.of("tag1"), List.of("col1"),
                Map.of("color", "red"), List.of(variant), "ai desc", ProductStatus.PUBLISHED,
                now, now, List.of(new Product.Translation("vi", "Tiện ích", "mô tả")));
    }

    @Test
    void toEntityMapsAllFieldsAndChildren() {
        ProductEntity entity = mapper.toEntity(sampleProduct());

        assertThat(entity.getProductId()).isEqualTo("p1");
        assertThat(entity.getStatus()).isEqualTo("PUBLISHED");
        assertThat(entity.getVariants()).hasSize(1);
        assertThat(entity.getVariants().get(0).getSkuId()).isEqualTo("sku-1");
        assertThat(entity.getVariants().get(0).getPrice()).isEqualByComparingTo("100");
        assertThat(entity.getVariants().get(0).getOriginalPrice()).isEqualByComparingTo("120");
        assertThat(entity.getVariants().get(0).getCurrency()).isEqualTo("VND");
        assertThat(entity.getTranslations()).hasSize(1);
        assertThat(entity.getTranslations().get(0).getName()).isEqualTo("Tiện ích");
    }

    @Test
    void roundTripEntityBackToDomain() {
        ProductEntity entity = mapper.toEntity(sampleProduct());

        Product domain = mapper.toDomain(entity);

        assertThat(domain.getProductId()).isEqualTo("p1");
        assertThat(domain.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(domain.categoryIds()).containsExactly("c1", "c2");
        assertThat(domain.tags()).containsExactly("tag1");
        assertThat(domain.collectionIds()).containsExactly("col1");
        assertThat(domain.variants()).hasSize(1);
        assertThat(domain.variants().get(0).price().amount()).isEqualByComparingTo("100");
        assertThat(domain.variants().get(0).originalPrice().amount()).isEqualByComparingTo("120");
        assertThat(domain.translations()).hasSize(1);
        assertThat(domain.translations().get(0).name()).isEqualTo("Tiện ích");
    }

    @Test
    void nullInputsReturnNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
