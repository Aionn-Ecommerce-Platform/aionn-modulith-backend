package com.aionn.catalog.infrastructure.integration;

import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.domain.model.Product;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.catalog.PricingQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingQueryAdapterTest {

    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
    private static final String MERCHANT_ID = "01HZMER0000000000000000001";
    private static final String ADMIN_ID = "01HZADM0000000000000000001";

    @Mock
    private ProductPersistencePort productRepository;

    @InjectMocks
    private PricingQueryAdapter adapter;

    private Product publishedProduct() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.categorize(List.of("01HZCAT0000000000000000001"));
        product.defineVariant("sku-1", Map.of("color", "red"), Money.of(new BigDecimal("100"), "VND"));
        product.publish(ADMIN_ID);
        product.pullEvents();
        return product;
    }

    @Test
    void resolvePricingReturnsEmptyForEmptyInput() {
        assertThat(adapter.resolvePricing(List.of())).isEmpty();
    }

    @Test
    void resolvePricingMapsRequestedSkus() {
        when(productRepository.findAllBySkuIds(List.of("sku-1"))).thenReturn(List.of(publishedProduct()));

        Map<String, PricingQueryPort.SkuPricing> result = adapter.resolvePricing(List.of("sku-1"));

        assertThat(result).containsKey("sku-1");
        PricingQueryPort.SkuPricing pricing = result.get("sku-1");
        assertThat(pricing.merchantId()).isEqualTo(MERCHANT_ID);
        assertThat(pricing.price()).isEqualByComparingTo("100");
        assertThat(pricing.active()).isTrue();
    }
}
