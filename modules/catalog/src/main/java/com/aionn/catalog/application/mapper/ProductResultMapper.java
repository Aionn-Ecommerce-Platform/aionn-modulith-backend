package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.policy.BestSellerPolicy;
import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductSoldCounterPersistencePort;
import com.aionn.catalog.application.port.out.review.ProductReviewPersistencePort;
import com.aionn.catalog.domain.model.Merchant;
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

    private final ProductSoldCounterPersistencePort productSoldCounterPersistencePort;
    private final BestSellerPolicy bestSellerPolicy;
    private final ProductReviewPersistencePort productReviewPersistencePort;
    private final MerchantPersistencePort merchantPersistencePort;

    public ProductResultMapper(
            ProductSoldCounterPersistencePort productSoldCounterPersistencePort,
            BestSellerPolicy bestSellerPolicy,
            ProductReviewPersistencePort productReviewPersistencePort,
            MerchantPersistencePort merchantPersistencePort
    ) {
        this.productSoldCounterPersistencePort = productSoldCounterPersistencePort;
        this.bestSellerPolicy = bestSellerPolicy;
        this.productReviewPersistencePort = productReviewPersistencePort;
        this.merchantPersistencePort = merchantPersistencePort;
    }

    public ProductResult toResult(Product product) {
        if (product == null) {
            return null;
        }
        long soldCount = productSoldCounterPersistencePort.getSoldCount(product.getProductId());
        double rating = productReviewPersistencePort.getAverageRating(product.getProductId());
        long reviewCount = productReviewPersistencePort.countByProductIdAndStatus(product.getProductId(), ReviewStatus.VISIBLE);
        Merchant merchant = merchantPersistencePort.findById(product.getMerchantId()).orElse(null);
        return toResult(product, soldCount, rating, reviewCount, merchant);
    }

    private ProductResult toResult(Product product, long soldCount, double rating, long reviewCount,
            Merchant merchant) {
        Locale locale = LocaleContextHolder.getLocale();
        String fullTag = locale.toLanguageTag();
        String language = locale.getLanguage();

        String name = product.getName();
        String description = product.getDescription();
        Product.Translation trans = product.translations().stream()
                .filter(t -> t.locale().equalsIgnoreCase(fullTag))
                .findFirst()
                .or(() -> product.translations().stream()
                        .filter(t -> t.locale().equalsIgnoreCase(language))
                        .findFirst())
                .orElse(null);
        if (trans != null) {
            name = trans.name();
            if (trans.description() != null) {
                description = trans.description();
            }
        }

        List<ProductResult.Variant> variants = product.variants().stream()
                .map(ProductResultMapper::toVariantResult)
                .toList();

        boolean isBestSeller = soldCount >= bestSellerPolicy.soldThreshold();

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
                description,
                product.getStatus() != null ? product.getStatus().name() : null,
                product.getCreatedAt(),
                product.getUpdatedAt(),
                soldCount,
                isBestSeller,
                rating,
                reviewCount,
                product.getSlug(),
                merchant == null ? null : merchant.getProvinceCode(),
                merchant == null ? null : merchant.getProvinceName());
    }

    public List<ProductResult> toResults(List<Product> products) {
        if (products == null) {
            return List.of();
        }
        List<String> productIds = products.stream().map(Product::getProductId).toList();
        Map<String, Long> soldCounts = productSoldCounterPersistencePort.getSoldCountsByProductIds(productIds);
        Map<String, ProductReviewPersistencePort.RatingStats> ratingStats = productReviewPersistencePort.getRatingStatsByProductIds(productIds);
        List<String> merchantIds = products.stream().map(Product::getMerchantId).distinct().toList();
        Map<String, Merchant> merchants = merchantPersistencePort.findByIds(merchantIds);
        return products.stream()
                .map(product -> {
                    long sold = soldCounts.getOrDefault(product.getProductId(), 0L);
                    var stats = ratingStats.get(product.getProductId());
                    double rating = stats != null ? stats.averageRating() : 0.0;
                    long reviews = stats != null ? stats.reviewCount() : 0L;
                    return toResult(product, sold, rating, reviews,
                            merchants.get(product.getMerchantId()));
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
