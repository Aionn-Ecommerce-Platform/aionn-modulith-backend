package com.aionn.catalog.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.catalog.domain.event.ProductEvents;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
public class Product extends AggregateRoot {

    private final String productId;
    private final String merchantId;
    private String name;
    private String brandId;
    private final List<String> categoryIds = new ArrayList<>();
    private final List<String> imageList = new ArrayList<>();
    private final List<String> tags = new ArrayList<>();
    private final List<String> collectionIds = new ArrayList<>();
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final List<ProductVariant> variants = new ArrayList<>();
    private String aiDescription;
    private ProductStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<Translation> translations = new ArrayList<>();

    public record Translation(String locale, String name, String aiDescription) {}

    public List<Translation> translations() {
        return List.copyOf(translations);
    }

    public Product(
            String productId,
            String merchantId,
            String name,
            String brandId,
            List<String> categoryIds,
            List<String> imageList,
            List<String> tags,
            List<String> collectionIds,
            Map<String, String> attributes,
            List<ProductVariant> variants,
            String aiDescription,
            ProductStatus status,
            Instant createdAt,
            Instant updatedAt,
            List<Translation> translations) {
        this.productId = productId;
        this.merchantId = merchantId;
        this.name = name;
        this.brandId = brandId;
        if (categoryIds != null)
            this.categoryIds.addAll(categoryIds);
        if (imageList != null)
            this.imageList.addAll(imageList);
        if (tags != null)
            this.tags.addAll(tags);
        if (collectionIds != null)
            this.collectionIds.addAll(collectionIds);
        if (attributes != null)
            this.attributes.putAll(attributes);
        if (variants != null)
            this.variants.addAll(variants);
        this.aiDescription = aiDescription;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        if (translations != null)
            this.translations.addAll(translations);
    }

    public static Product create(String productId, String merchantId, String name) {
        return create(productId, merchantId, name, Clock.systemUTC());
    }

    public static Product create(String productId, String merchantId, String name, Clock clock) {
        Guard.require(name != null && !name.isBlank(),
                () -> new CatalogException(CatalogErrorCode.INVALID_ARGUMENT, "name must not be blank"));
        Instant now = clock.instant();
        Product product = new Product(productId, merchantId, name.trim(), null,
                List.of(), List.of(), List.of(), List.of(), Map.of(), List.of(),
                null, ProductStatus.DRAFT, now, now, List.of());
        product.registerEvent(new ProductEvents.ProductCreated(productId, merchantId, name, now));
        return product;
    }

    public void ensureOwnedBy(String merchantId) {
        Guard.require(this.merchantId.equals(merchantId),
                () -> new CatalogException(CatalogErrorCode.PRODUCT_FORBIDDEN));
    }

    public void defineVariant(String skuId, Map<String, String> attributeValues, Money price) {
        defineVariant(skuId, attributeValues, price, Clock.systemUTC());
    }

    public void defineVariant(String skuId, Map<String, String> attributeValues, Money price, Clock clock) {
        for (ProductVariant existing : variants) {
            Guard.require(!existing.skuId().equals(skuId),
                    () -> new CatalogException(CatalogErrorCode.PRODUCT_VARIANT_DUPLICATE,
                            "Variant id already exists"));
            Guard.require(!existing.matches(attributeValues),
                    () -> new CatalogException(CatalogErrorCode.PRODUCT_VARIANT_DUPLICATE,
                            "Variant attribute combination already exists"));
        }
        variants.add(new ProductVariant(skuId, attributeValues, price));
        touch(clock);
        registerEvent(new ProductEvents.ProductVariantDefined(productId, skuId, Map.copyOf(attributeValues), updatedAt));
    }

    public void removeVariant(String skuId) {
        removeVariant(skuId, Clock.systemUTC());
    }

    public void removeVariant(String skuId, Clock clock) {
        ProductVariant existing = findVariant(skuId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        variants.remove(existing);
        touch(clock);
        registerEvent(new ProductEvents.ProductVariantRemoved(productId, skuId, merchantId, updatedAt, updatedAt));
    }

    public void changeVariantPrice(String skuId, Money newPrice) {
        changeVariantPrice(skuId, newPrice, Clock.systemUTC());
    }

    public void changeVariantPrice(String skuId, Money newPrice, Clock clock) {
        ProductVariant existing = findVariant(skuId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        BigDecimal oldAmount = existing.price() == null ? null : existing.price().amount();
        existing.setPrice(newPrice);
        touch(clock);
        registerEvent(new ProductEvents.ProductVariantPriceChanged(
                productId, skuId, oldAmount, newPrice.amount(), newPrice.currency(), updatedAt));
    }

    public void updateMedia(List<String> images) {
        updateMedia(images, Clock.systemUTC());
    }

    public void updateMedia(List<String> images, Clock clock) {
        this.imageList.clear();
        if (images != null) {
            this.imageList.addAll(images);
        }
        touch(clock);
        registerEvent(new ProductEvents.ProductMediaUpdated(productId, List.copyOf(this.imageList), updatedAt));
    }

    public void categorize(List<String> categoryIds) {
        categorize(categoryIds, Clock.systemUTC());
    }

    public void categorize(List<String> categoryIds, Clock clock) {
        Guard.require(categoryIds != null && !categoryIds.isEmpty(),
                () -> new CatalogException(CatalogErrorCode.PRODUCT_CATEGORY_REQUIRED));
        this.categoryIds.clear();
        this.categoryIds.addAll(categoryIds);
        touch(clock);
        registerEvent(new ProductEvents.ProductCategorized(productId, List.copyOf(this.categoryIds), updatedAt));
    }

    public void assignBrand(String brandId) {
        assignBrand(brandId, Clock.systemUTC());
    }

    public void assignBrand(String brandId, Clock clock) {
        Guard.require(brandId != null && !brandId.isBlank(),
                () -> new CatalogException(CatalogErrorCode.INVALID_ARGUMENT, "brandId must not be blank"));
        this.brandId = brandId;
        touch(clock);
        registerEvent(new ProductEvents.ProductBrandAssigned(productId, brandId, updatedAt));
    }

    public void publish(String adminId) {
        publish(adminId, Clock.systemUTC());
    }

    public void publish(String adminId, Clock clock) {
        Guard.require(!variants.isEmpty(),
                () -> new CatalogException(CatalogErrorCode.PRODUCT_PUBLISH_REQUIREMENTS,
                        "Cannot publish without at least one SKU"));
        Guard.require(!categoryIds.isEmpty(),
                () -> new CatalogException(CatalogErrorCode.PRODUCT_PUBLISH_REQUIREMENTS,
                        "Cannot publish without a category"));
        ensureTransitionAllowed(ProductStatus.PUBLISHED);
        this.status = ProductStatus.PUBLISHED;
        touch(clock);
        registerEvent(new ProductEvents.ProductPublished(productId, adminId, updatedAt, updatedAt));
    }

    public void submitForReview(String ownerId) {
        submitForReview(ownerId, Clock.systemUTC());
    }

    public void submitForReview(String ownerId, Clock clock) {
        ensureTransitionAllowed(ProductStatus.PENDING_REVIEW);
        this.status = ProductStatus.PENDING_REVIEW;
        touch(clock);
        registerEvent(new ProductEvents.ProductSubmittedForReview(productId, ownerId, updatedAt));
    }

    public void reject(String adminId, String reasonCode, String feedback) {
        reject(adminId, reasonCode, feedback, Clock.systemUTC());
    }

    public void reject(String adminId, String reasonCode, String feedback, Clock clock) {
        ensureTransitionAllowed(ProductStatus.REJECTED);
        this.status = ProductStatus.REJECTED;
        touch(clock);
        registerEvent(new ProductEvents.ProductRejected(productId, adminId, reasonCode, feedback, updatedAt));
    }

    public void deactivate(String reason) {
        deactivate(reason, Clock.systemUTC());
    }

    public void deactivate(String reason, Clock clock) {
        ensureTransitionAllowed(ProductStatus.HIDDEN);
        this.status = ProductStatus.HIDDEN;
        touch(clock);
        registerEvent(new ProductEvents.ProductDeactivated(productId, merchantId, reason, updatedAt, updatedAt));
    }

    public void restore() {
        restore(Clock.systemUTC());
    }

    public void restore(Clock clock) {
        ensureTransitionAllowed(ProductStatus.PUBLISHED);
        this.status = ProductStatus.PUBLISHED;
        touch(clock);
        registerEvent(new ProductEvents.ProductRestored(productId, updatedAt, updatedAt));
    }

    public void emergencyTakedown(String adminId, String reason) {
        emergencyTakedown(adminId, reason, Clock.systemUTC());
    }

    public void emergencyTakedown(String adminId, String reason, Clock clock) {
        ensureTransitionAllowed(ProductStatus.TAKEN_DOWN);
        this.status = ProductStatus.TAKEN_DOWN;
        touch(clock);
        registerEvent(new ProductEvents.ProductEmergencyTakedown(productId, adminId, reason, updatedAt, updatedAt));
    }

    public void updateAiMetadata(List<String> tags, String aiDescription) {
        updateAiMetadata(tags, aiDescription, Clock.systemUTC());
    }

    public void updateAiMetadata(List<String> tags, String aiDescription, Clock clock) {
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
        if (aiDescription != null) {
            this.aiDescription = aiDescription;
        }
        touch(clock);
        registerEvent(new ProductEvents.ProductMetadataUpdated(productId, List.copyOf(this.tags), this.aiDescription,
                updatedAt));
    }

    public void assignToCollections(List<String> collectionIds) {
        assignToCollections(collectionIds, Clock.systemUTC());
    }

    public void assignToCollections(List<String> collectionIds, Clock clock) {
        this.collectionIds.clear();
        if (collectionIds != null) {
            this.collectionIds.addAll(collectionIds);
        }
        touch(clock);
        registerEvent(new ProductEvents.ProductCollectionAssigned(productId, List.copyOf(this.collectionIds), updatedAt));
    }

    public void defineAttributes(Map<String, String> attributes) {
        defineAttributes(attributes, Clock.systemUTC());
    }

    public void defineAttributes(Map<String, String> attributes, Clock clock) {
        this.attributes.clear();
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
        touch(clock);
        registerEvent(new ProductEvents.ProductAttributesDefined(productId, Map.copyOf(this.attributes), updatedAt));
    }

    public Optional<ProductVariant> findVariant(String skuId) {
        return variants.stream().filter(v -> v.skuId().equals(skuId)).findFirst();
    }

    public List<ProductVariant> variants() {
        return Collections.unmodifiableList(variants);
    }

    public List<String> categoryIds() {
        return List.copyOf(categoryIds);
    }

    public List<String> imageList() {
        return List.copyOf(imageList);
    }

    public List<String> tags() {
        return List.copyOf(tags);
    }

    public List<String> collectionIds() {
        return List.copyOf(collectionIds);
    }

    public Map<String, String> attributes() {
        return Map.copyOf(attributes);
    }

    private void ensureTransitionAllowed(ProductStatus next) {
        Guard.require(status.canTransitionTo(next),
                () -> new CatalogException(CatalogErrorCode.PRODUCT_INVALID_TRANSITION,
                        "Cannot transition from " + status + " to " + next));
    }

    private void touch(Clock clock) {
        this.updatedAt = clock.instant();
    }

    @Override
    protected String aggregateId() {
        return productId;
    }
}
