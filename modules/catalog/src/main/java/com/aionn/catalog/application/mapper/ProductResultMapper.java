package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.policy.CatalogProductPolicy;
import com.aionn.catalog.application.port.out.product.ProductSoldCounterPersistencePort;
import com.aionn.catalog.application.port.out.review.ProductReviewPersistencePort;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.catalog.domain.valueobject.ReviewStatus;
import com.aionn.sharedkernel.domain.vo.Money;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ProductResultMapper {

    private final ProductSoldCounterPersistencePort soldCounterRepository;
    private final CatalogProductPolicy productPolicy;
    private final ProductReviewPersistencePort reviewRepository;

    public ProductResultMapper(
            ProductSoldCounterPersistencePort soldCounterRepository,
            CatalogProductPolicy productPolicy,
            ProductReviewPersistencePort reviewRepository
    ) {
        this.soldCounterRepository = soldCounterRepository;
        this.productPolicy = productPolicy;
        this.reviewRepository = reviewRepository;
    }

    public ProductResult toResult(Product product) {
        if (product == null) {
            return null;
        }
        long soldCount = soldCounterRepository.getSoldCount(product.getProductId());
        double rating = reviewRepository.getAverageRating(product.getProductId());
        long reviewCount = reviewRepository.countByProductIdAndStatus(product.getProductId(), ReviewStatus.VISIBLE);
        return toResult(product, soldCount, rating, reviewCount);
    }

    private ProductResult toResult(Product product, long soldCount, double rating, long reviewCount) {
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

        boolean isBestSeller = soldCount >= productPolicy.getBestSellerThreshold();

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
                product.getUpdatedAt(),
                soldCount,
                isBestSeller,
                rating,
                reviewCount,
                product.getSlug());
    }

    public List<ProductResult> toResults(List<Product> products) {
        if (products == null) {
            return List.of();
        }
        List<String> productIds = products.stream().map(Product::getProductId).toList();
        Map<String, Long> soldCounts = soldCounterRepository.getSoldCountsByProductIds(productIds);
        Map<String, ProductReviewPersistencePort.RatingStats> ratingStats = reviewRepository.getRatingStatsByProductIds(productIds);
        return products.stream()
                .map(product -> {
                    long sold = soldCounts.getOrDefault(product.getProductId(), 0L);
                    var stats = ratingStats.get(product.getProductId());
                    double rating = stats != null ? stats.averageRating() : 0.0;
                    long reviews = stats != null ? stats.reviewCount() : 0L;
                    return toResult(product, sold, rating, reviews);
                })
                .toList();
    }

    public PageResult<ProductResult> toPageResult(PageResult<Product> page) {
        return new PageResult<>(
                toResults(page.content()),
                page.page(),
                page.size(),
                page.totalElements());
    }

    private static ProductResult.Variant toVariantResult(ProductVariant variant) {
        Money price = variant.price();
        Money original = variant.originalPrice();
        BigDecimal amount = price != null ? price.amount() : null;
        BigDecimal originalAmount = original != null ? original.amount() : null;
        String currency = price != null ? price.currency() : null;
        ProductVariant.ParcelDimensions parcel = variant.parcel();
        return new ProductResult.Variant(variant.skuId(), variant.attributeValues(), amount,
                originalAmount, currency,
                parcel == null ? null : parcel.weightGram(),
                parcel == null ? null : parcel.lengthCm(),
                parcel == null ? null : parcel.widthCm(),
                parcel == null ? null : parcel.heightCm());
    }
}
