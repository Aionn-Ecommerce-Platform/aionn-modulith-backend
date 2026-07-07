package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.application.port.out.product.ProductSoldCounterPersistencePort;
import com.aionn.catalog.application.port.out.review.ProductReviewPersistencePort;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.sharedkernel.domain.vo.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProductSearchDocumentMapper {

    private final ProductReviewPersistencePort reviewRepository;
    private final ProductSoldCounterPersistencePort soldCounterRepository;

    public ProductSearchDocument toSearchDocument(Product product, Map<String, String> filterableAttributes) {
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

        double rating = reviewRepository.getAverageRating(product.getProductId());
        long soldCount = soldCounterRepository.getSoldCount(product.getProductId());

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
                product.getStatus().name(),
                product.getUpdatedAt(),
                rating,
                soldCount);
    }
}
