package com.aionn.catalog.infrastructure.integration;

import com.aionn.catalog.domain.model.ProductVariant;

import com.aionn.catalog.infrastructure.integration.catalog.PricingQueryAdapter;

import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.domain.model.Merchant;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.infrastructure.config.properties.CatalogProductProperties;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.contracts.port.catalog.PricingQueryPort;
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
import static com.aionn.sharedkernel.testing.fixture.TestClocks.FIXED;

@ExtendWith(MockitoExtension.class)
class PricingQueryAdapterTest {

    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
    private static final String MERCHANT_ID = "01HZMER0000000000000000001";
    private static final String ADMIN_ID = "01HZADM0000000000000000001";
    private static final ProductVariant.ParcelDimensions TEST_PARCEL = new ProductVariant.ParcelDimensions(500, BigDecimal.valueOf(15), BigDecimal.valueOf(10), BigDecimal.valueOf(5));

    @Mock
    private ProductPersistencePort productPersistencePort;
    @Mock
    private MerchantPersistencePort merchantPersistencePort;
    @Mock
    private CatalogProductProperties productProperties;

    @InjectMocks
    private PricingQueryAdapter adapter;

    private Product publishedProduct() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget", FIXED);
        product.categorize(List.of("01HZCAT0000000000000000001"), FIXED);
        product.defineVariant("sku-1", Map.of("color", "red"), Money.of(new BigDecimal("100"), "VND"), TEST_PARCEL, FIXED);
        product.publish(ADMIN_ID, FIXED);
        product.pullEvents();
        return product;
    }

    @Test
    void resolvePricingReturnsEmptyForEmptyInput() {
        assertThat(adapter.resolvePricing(List.of())).isEmpty();
    }

    @Test
    void resolvePricingMapsRequestedSkus() {
        Merchant merchant = Merchant.register(MERCHANT_ID, "owner-1", "Acme", BigDecimal.valueOf(0.05), FIXED);
        merchant.activate(ADMIN_ID, "test", FIXED);
        when(merchantPersistencePort.findById(MERCHANT_ID)).thenReturn(java.util.Optional.of(merchant));
        when(productPersistencePort.findAllBySkuIds(List.of("sku-1"))).thenReturn(List.of(publishedProduct()));

        Map<String, PricingQueryPort.SkuPricing> result = adapter.resolvePricing(List.of("sku-1"));

        assertThat(result).containsKey("sku-1");
        PricingQueryPort.SkuPricing pricing = result.get("sku-1");
        assertThat(pricing.merchantId()).isEqualTo(MERCHANT_ID);
        assertThat(pricing.price()).isEqualByComparingTo("100");
        assertThat(pricing.active()).isTrue();
    }
}
