package com.aionn.catalog.infrastructure.integration.catalog;

import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.catalog.domain.valueobject.MerchantStatus;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.catalog.infrastructure.config.properties.CatalogProductProperties;
import com.aionn.contracts.port.catalog.PricingQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PricingQueryAdapter implements PricingQueryPort {

    private final ProductPersistencePort productRepository;
    private final MerchantPersistencePort merchantRepository;
    private final CatalogProductProperties productProperties;

    @Override
    @Transactional(readOnly = true)
    public Map<String, SkuPricing> resolvePricing(List<String> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Map.of();
        }
        Map<String, SkuPricing> result = new LinkedHashMap<>();
        List<Product> products = productRepository.findAllBySkuIds(skuIds);
        for (Product product : products) {
            boolean merchantActive = merchantRepository.findById(product.getMerchantId())
                    .map(m -> m.getStatus() == MerchantStatus.ACTIVE)
                    .orElse(false);
            boolean active = product.getStatus() == ProductStatus.PUBLISHED && merchantActive;
            for (ProductVariant variant : product.variants()) {
                if (!skuIds.contains(variant.skuId())) {
                    continue;
                }
                String currency = variant.price() == null
                        ? productProperties.defaultCurrency()
                        : variant.price().currency();
                BigDecimal price = variant.price() == null ? null : variant.price().amount();
                ProductVariant.ParcelDimensions pd = variant.parcel();
                SkuPricing.Parcel parcel = pd == null ? null
                        : new SkuPricing.Parcel(pd.weightGram(), pd.lengthCm(), pd.widthCm(), pd.heightCm());
                result.put(variant.skuId(), new SkuPricing(
                        variant.skuId(), product.getMerchantId(), price, currency, active,
                        product.categoryIds(), parcel, product.getName(), firstImage(product),
                        variantLabel(variant)));
            }
        }
        return result;
    }

    private static String firstImage(Product product) {
        if (product.getImageList() == null) {
            return null;
        }
        return product.getImageList().stream()
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static String variantLabel(ProductVariant variant) {
        if (variant.attributeValues() == null || variant.attributeValues().isEmpty()) {
            return null;
        }
        return String.join(" / ", variant.attributeValues().values());
    }
}
