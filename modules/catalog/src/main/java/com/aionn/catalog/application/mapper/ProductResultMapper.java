package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.sharedkernel.domain.vo.Money;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Component
public class ProductResultMapper {

    public ProductResult toResult(Product product) {
        if (product == null) {
            return null;
        }
        Locale locale = LocaleContextHolder.getLocale();
        String fullTag = locale.toLanguageTag();
        String language = locale.getLanguage();

        String name = product.getName();
        String aiDescription = product.getAiDescription();
        Product.Translation trans = product.translations().stream()
                .filter(t -> t.locale().equalsIgnoreCase(fullTag))
                .findFirst()
                .or(() -> product.translations().stream()
                        .filter(t -> t.locale().equalsIgnoreCase(language))
                        .findFirst())
                .orElse(null);
        if (trans != null) {
            name = trans.name();
            if (trans.aiDescription() != null) {
                aiDescription = trans.aiDescription();
            }
        }

        List<ProductResult.Variant> variants = product.variants().stream()
                .map(ProductResultMapper::toVariantResult)
                .toList();

        return new ProductResult(
                product.getProductId(),
                product.getMerchantId(),
                name,
                product.getBrandId(),
                product.categoryIds(),
                product.imageList(),
                product.tags(),
                product.attributes(),
                variants,
                aiDescription,
                product.getStatus() != null ? product.getStatus().name() : null,
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    private static ProductResult.Variant toVariantResult(ProductVariant variant) {
        Money price = variant.price();
        BigDecimal amount = price != null ? price.amount() : null;
        String currency = price != null ? price.currency() : null;
        return new ProductResult.Variant(variant.skuId(), variant.attributeValues(), amount, currency);
    }
}
