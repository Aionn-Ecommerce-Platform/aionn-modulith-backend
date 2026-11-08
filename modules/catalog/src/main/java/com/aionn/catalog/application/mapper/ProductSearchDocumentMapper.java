package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.sharedkernel.domain.vo.Money;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure mapper: no business logic, queries or side effects; rating/soldCount come from the caller. */
@Component
public class ProductSearchDocumentMapper {

    public ProductSearchDocument toSearchDocument(
            Product product,
            Map<String, String> filterableAttributes,
            double rating,
            long soldCount) {

        List<BigDecimal> prices = product.variants().stream()
                .map(ProductVariant::price)
                .filter(java.util.Objects::nonNull)
                .map(Money::amount)
                .toList();
        BigDecimal priceFrom = prices.stream().reduce(BigDecimal::min).orElse(null);
        BigDecimal priceTo = prices.stream().reduce(BigDecimal::max).orElse(null);
        String currency = product.variants().stream()
                .map(ProductVariant::price)
                .filter(java.util.Objects::nonNull)
                .map(Money::currency)
                .findFirst()
                .orElse(null);
        // True when a variant's original price exceeds its sell price (standing discount).
        boolean hasDiscount = product.variants().stream()
                .anyMatch(v -> {
                    Money price = v.price();
                    Money original = v.originalPrice();
                    return price != null && original != null
                            && original.amount() != null && price.amount() != null
                            && original.amount().compareTo(price.amount()) > 0;
                });

        Locale locale = LocaleContextHolder.getLocale();
        String name = product.getName();
        String aiDescription = product.getAiDescription();
        Product.Translation trans = product.translations().stream()
                .filter(t -> t.locale().equalsIgnoreCase(locale.getLanguage()))
                .findFirst()
                .orElse(null);
        if (trans != null) {
            name = trans.name();
            if (trans.aiDescription() != null) {
                aiDescription = trans.aiDescription();
            }
        }

        return new ProductSearchDocument(
                product.getProductId(),
                product.getMerchantId(),
                name,
                aiDescription,
                product.getBrandId(),
                product.categoryIds(),
                product.collectionIds(),
                product.tags(),
                product.imageList(),
                filterableAttributes == null ? Map.of() : filterableAttributes,
                priceFrom,
                priceTo,
                currency,
                hasDiscount,
                product.getStatus().name(),
                product.getUpdatedAt(),
                rating,
                soldCount);
    }
}
