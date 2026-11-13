package com.aionn.catalog.application.service;

import com.aionn.catalog.application.dto.product.command.AssignCollectionsCommand;
import com.aionn.catalog.application.dto.product.command.ChangeVariantParcelCommand;
import com.aionn.catalog.application.dto.product.command.EmergencyTakedownCommand;
import com.aionn.catalog.application.dto.product.command.RecordSearchIntentCommand;
import com.aionn.catalog.application.dto.product.command.RemoveVariantCommand;
import com.aionn.catalog.application.dto.product.command.UpdateAiMetadataCommand;
import com.aionn.catalog.application.dto.product.command.UpdateMediaCommand;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.command.AssignBrandCommand;
import com.aionn.catalog.application.dto.product.command.AssignCategoriesCommand;
import com.aionn.catalog.application.dto.product.command.BulkPriceUpdateCommand;
import com.aionn.catalog.application.dto.product.command.ChangeVariantPriceCommand;
import com.aionn.catalog.application.dto.product.command.CloneProductCommand;
import com.aionn.catalog.application.dto.product.command.CreateProductCommand;
import com.aionn.catalog.application.dto.product.command.DeactivateProductCommand;
import com.aionn.catalog.application.dto.product.command.DefineAttributesCommand;
import com.aionn.catalog.application.dto.product.command.DefineVariantCommand;
import com.aionn.catalog.application.dto.product.command.PublishProductCommand;
import com.aionn.catalog.application.dto.product.command.RejectProductCommand;
import com.aionn.catalog.application.dto.product.command.RestoreProductCommand;
import com.aionn.catalog.application.dto.product.command.SubmitForReviewCommand;
import com.aionn.catalog.application.dto.product.result.BulkPriceUpdateResult;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.dto.search.ProductSearchCriteria;
import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.application.dto.search.ProductSearchResult;
import com.aionn.catalog.application.mapper.ProductResultMapper;
import com.aionn.catalog.application.mapper.ProductSearchDocumentMapper;
import com.aionn.catalog.application.policy.BulkPriceUpdatePolicy;
import com.aionn.catalog.application.policy.ProductClonePolicy;
import com.aionn.catalog.application.policy.SearchIndexBatchPolicy;
import com.aionn.catalog.application.port.out.attribute.AttributeTemplatePersistencePort;
import com.aionn.catalog.application.port.out.integration.CatalogIntegrationEventPublisherPort;
import com.aionn.catalog.application.port.out.search.ProductSearchIndex;
import com.aionn.catalog.application.port.out.product.ProductSoldCounterPersistencePort;
import com.aionn.catalog.application.port.out.review.ProductReviewPersistencePort;
import com.aionn.catalog.domain.model.AttributeTemplate;
import com.aionn.catalog.application.port.out.brand.BrandPersistencePort;
import com.aionn.catalog.application.port.out.category.CategoryPersistencePort;
import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.application.port.out.product.UserBrowsingHistoryPersistencePort;
import com.aionn.catalog.domain.model.UserBrowsingHistory;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Brand;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.catalog.domain.valueobject.BrandStatus;
import com.aionn.catalog.domain.valueobject.MerchantStatus;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductPersistencePort productPersistencePort;
    private final UserBrowsingHistoryPersistencePort userBrowsingHistoryPersistencePort;
    private final MerchantPersistencePort merchantPersistencePort;
    private final BrandPersistencePort brandPersistencePort;
    private final CategoryPersistencePort categoryPersistencePort;
    private final ProductSearchIndex catalogSearchIndex;
    private final ProductSearchDocumentMapper searchDocumentMapper;
    private final AttributeTemplatePersistencePort attributeTemplatePersistencePort;
    private final ProductReviewPersistencePort productReviewPersistencePort;
    private final ProductSoldCounterPersistencePort productSoldCounterPersistencePort;
    private final ProductResultMapper productResultMapper;
    private final BulkPriceUpdatePolicy bulkPriceUpdatePolicy;
    private final ProductClonePolicy productClonePolicy;
    private final SearchIndexBatchPolicy searchIndexBatchPolicy;
    private final EventPublisher eventPublisher;
    private final CatalogIntegrationEventPublisherPort integrationEventPublisher;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public Product create(CreateProductCommand command) {
        requireActiveMerchant(command.merchantId());
        Product product = Product.create(IdGenerator.ulid(), command.merchantId(), command.name(), clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    public Product clone(CloneProductCommand command) {
        Product source = ownedBy(command.productId(), command.merchantId());
        requireActiveMerchant(command.merchantId());
        if (source.getStatus() == ProductStatus.TAKEN_DOWN
                || source.getStatus() == ProductStatus.REJECTED) {
            throw new CatalogException(CatalogErrorCode.PRODUCT_INVALID_TRANSITION,
                    "Products under moderation cannot be cloned");
        }
        String newId = IdGenerator.ulid();
        List<ProductVariant> clonedVariants = source.variants().stream()
                .map(v -> new ProductVariant(
                        IdGenerator.ulid(),
                        v.attributeValues(),
                        v.price(),
                        v.originalPrice(),
                        v.parcel()))
                .toList();
        Product clone = new Product(
                newId,
                command.merchantId(),
                source.getName() + productClonePolicy.nameSuffix(),
                source.getBrandId(),
                source.categoryIds(),
                source.imageList(),
                source.tags(),
                source.collectionIds(),
                source.attributes(),
                clonedVariants,
                source.getAiDescription(),
                ProductStatus.DRAFT,
                clock.instant(),
                clock.instant(),
                0L,
                source.translations());
        Product saved = productPersistencePort.save(clone);
        eventPublisher.publish(clone.pullEvents());
        return saved;
    }

    public Product defineVariant(DefineVariantCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        Money price = Money.of(command.price(), command.currency());
        ProductVariant.ParcelDimensions parcel = command.weightGram() == null
                && command.lengthCm() == null && command.widthCm() == null && command.heightCm() == null
                        ? null
                        : new ProductVariant.ParcelDimensions(
                                command.weightGram(), command.lengthCm(), command.widthCm(), command.heightCm());
        product.defineVariant(command.skuId(), command.attributeValues(), price, parcel, clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    public Product removeVariant(RemoveVariantCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.removeVariant(command.skuId(), clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    public Product updateMedia(UpdateMediaCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.updateMedia(command.images(), clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    public Product updateAiMetadata(
            UpdateAiMetadataCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.updateAiMetadata(command.tags(), command.aiDescription(), clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    public Product assignCollections(
            AssignCollectionsCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.assignToCollections(command.collectionIds(), clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    public Product emergencyTakedown(
            EmergencyTakedownCommand command) {
        Product product = required(command.productId());
        product.emergencyTakedown(command.adminId(), command.reason(), clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PageResult<Product> search(String keyword, OffsetPagination pagination) {
        ProductSearchCriteria criteria = new ProductSearchCriteria(
                keyword, null, ProductStatus.PUBLISHED, List.of(), List.of(),
                null, null, Map.of(), ProductSearchCriteria.Sort.RELEVANCE,
                pagination.page(), pagination.size());
        Optional<ProductSearchIndex.SearchHits> hits = catalogSearchIndex.search(criteria);
        if (hits.isPresent()) {
            ProductSearchIndex.SearchHits result = hits.get();
            List<Product> products = transactionTemplate.execute(
                    status -> productPersistencePort.findByIdsPreserveOrder(result.productIds()));
            return new PageResult<>(products, pagination.page(), pagination.size(), result.totalHits());
        }
        return transactionTemplate.execute(status -> {
            int offset = pagination.page() * pagination.size();
            List<Product> products = productPersistencePort.searchPublished(keyword, pagination.size(), offset);
            long total = productPersistencePort.countSearchPublished(keyword);
            return new PageResult<>(products, pagination.page(), pagination.size(), total);
        });
    }

    public Product changeVariantPrice(ChangeVariantPriceCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.changeVariantPrice(command.skuId(), Money.of(command.newPrice(), command.currency()), clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    public Product changeVariantParcel(
            ChangeVariantParcelCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        ProductVariant.ParcelDimensions parcel = new ProductVariant.ParcelDimensions(
                command.weightGram(), command.lengthCm(), command.widthCm(), command.heightCm());
        product.changeVariantParcel(command.skuId(), parcel, clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    public BulkPriceUpdateResult bulkPriceUpdate(BulkPriceUpdateCommand command) {
        if (command.items() == null || command.items().isEmpty()) {
            return new BulkPriceUpdateResult(0, 0, List.of());
        }
        int max = bulkPriceUpdatePolicy.maxUpdateSize();
        if (command.items().size() > max) {
            throw new CatalogException(CatalogErrorCode.PRODUCT_BULK_TOO_LARGE,
                    "Bulk price update exceeds allowed batch size: " + max);
        }
        int updated = 0;
        int skipped = 0;
        List<String> failed = new ArrayList<>();
        for (BulkPriceUpdateCommand.Item item : command.items()) {
            try {
                Product product = ownedBy(item.productId(), command.merchantId());
                product.changeVariantPrice(item.skuId(), Money.of(item.newPrice(), item.currency()), clock);
                productPersistencePort.save(product);
                eventPublisher.publish(product.pullEvents());
                updated++;
            } catch (CatalogException e) {
                failed.add(item.skuId());
                skipped++;
            }
        }
        return new BulkPriceUpdateResult(updated, skipped, failed);
    }

    public Product assignBrand(AssignBrandCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        Brand brand = brandPersistencePort.findById(command.brandId())
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.BRAND_NOT_FOUND));
        if (brand.getStatus() != BrandStatus.ACTIVE) {
            throw new CatalogException(CatalogErrorCode.PRODUCT_BRAND_NOT_APPROVED);
        }
        product.assignBrand(command.brandId(), clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    public Product categorize(AssignCategoriesCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        if (command.categoryIds() == null || command.categoryIds().isEmpty()) {
            throw new CatalogException(CatalogErrorCode.PRODUCT_CATEGORY_REQUIRED);
        }
        for (String categoryId : command.categoryIds()) {
            categoryPersistencePort.findById(categoryId)
                    .orElseThrow(() -> new CatalogException(CatalogErrorCode.CATEGORY_NOT_FOUND,
                            "Category not found: " + categoryId));
        }
        product.categorize(command.categoryIds(), clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    public Product defineAttributes(DefineAttributesCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.defineAttributes(command.attributes(), clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    public Product publish(PublishProductCommand command) {
        requireActiveMerchant(command.actorId());
        Product product = ownedBy(command.productId(), command.actorId());
        product.publish(command.actorId(), clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    public Product submitForReview(SubmitForReviewCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.submitForReview(command.merchantId(), clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    public Product reject(RejectProductCommand command) {
        Product product = required(command.productId());
        product.reject(command.adminId(), command.reasonCode(), command.feedback(), clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    public Product deactivate(DeactivateProductCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.deactivate(command.reason(), clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    public Product restore(RestoreProductCommand command) {
        Product product = required(command.productId());
        product.restore(clock);
        Product saved = productPersistencePort.save(product);
        eventPublisher.publish(product.pullEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public Product get(String productId) {
        return required(productId);
    }

    @Transactional(readOnly = true)
    public List<Product> getBySkuIds(List<String> skuIds) {
        return productPersistencePort.findAllBySkuIds(skuIds);
    }

    @Transactional(readOnly = true)
    public PageResult<Product> listByMerchant(String merchantId, OffsetPagination pagination) {
        List<Product> content = productPersistencePort.listByMerchant(merchantId, pagination);
        long total = productPersistencePort.countByMerchant(merchantId);
        return new PageResult<>(content, pagination.page(), pagination.size(), total);
    }

    @Transactional(readOnly = true)
    public PageResult<Product> listByStatus(ProductStatus status, OffsetPagination pagination) {
        List<Product> content = productPersistencePort.listByStatus(status, pagination);
        long total = productPersistencePort.countByStatus(status);
        return new PageResult<>(content, pagination.page(), pagination.size(), total);
    }

    @Transactional(readOnly = true)
    public List<Product> getRelatedProducts(String productId, int limit) {
        Product product = required(productId);
        return productPersistencePort.findRelatedProducts(
                product.getProductId(),
                product.getBrandId(),
                product.categoryIds(),
                limit);
    }

    @Transactional(readOnly = true)
    public List<Product> getPopularProducts(int limit) {
        return popularProducts(limit);
    }

    private List<Product> popularProducts(int limit) {
        return productPersistencePort.findPopularProducts(limit);
    }

    @Transactional(readOnly = true)
    public List<Product> getPersonalizedProducts(String userId, List<String> categoryIds, List<String> brandIds,
            int limit) {
        List<String> activeCategoryIds = categoryIds != null ? categoryIds : List.of();
        List<String> activeBrandIds = brandIds != null ? brandIds : List.of();

        if (isRealUser(userId)) {
            Optional<UserBrowsingHistory> history = userBrowsingHistoryPersistencePort.findByUserId(userId);
            if (history.isPresent()) {
                activeCategoryIds = history.get().getCategoryIds();
                activeBrandIds = history.get().getBrandIds();
            }
        }

        if (activeCategoryIds.isEmpty() && activeBrandIds.isEmpty()) {
            return popularProducts(limit);
        }

        List<Product> products = productPersistencePort.findPersonalizedProducts(activeCategoryIds, activeBrandIds, limit);
        if (products.isEmpty()) {
            return popularProducts(limit);
        }
        return products;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackProductView(String productId, String userId) {
        if (!isRealUser(userId)) {
            return;
        }
        Product product = required(productId);
        UserBrowsingHistory history = userBrowsingHistoryPersistencePort.findByUserId(userId)
                .orElseGet(() -> UserBrowsingHistory.create(userId));
        history.trackView(product.categoryIds(), product.getBrandId());
        userBrowsingHistoryPersistencePort.save(history);
        integrationEventPublisher.publishProductViewed(
                product.getProductId(),
                userId,
                product.getBrandId(),
                product.categoryIds(),
                clock.instant());
    }

    /**
     * Records a user's explicit search intent as a behavioural signal for the recommendation module.
     * Only published when there is something to learn from - a keyword or a chosen facet - and only
     * for real users; anonymous browsing is not personalised.
     */
    public void recordSearchIntent(RecordSearchIntentCommand command) {
        if (!isRealUser(command.userId())) {
            return;
        }
        boolean hasKeyword = command.keyword() != null && !command.keyword().isBlank();
        boolean hasFacets = (command.categoryIds() != null && !command.categoryIds().isEmpty())
                || (command.brandIds() != null && !command.brandIds().isEmpty());
        if (!hasKeyword && !hasFacets) {
            return;
        }
        integrationEventPublisher.publishSearchPerformed(
                command.userId(),
                hasKeyword ? command.keyword().trim() : null,
                command.categoryIds() == null ? List.of() : command.categoryIds(),
                command.brandIds() == null || command.brandIds().isEmpty() ? null : command.brandIds().get(0),
                clock.instant());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ProductSearchResult searchCatalog(ProductSearchCriteria criteria) {
        Optional<ProductSearchIndex.SearchHits> hitsOpt = catalogSearchIndex.search(criteria);
        if (hitsOpt.isPresent()) {
            ProductSearchIndex.SearchHits hits = hitsOpt.get();
            List<Product> products = transactionTemplate.execute(
                    status -> productPersistencePort.findByIdsPreserveOrder(hits.productIds()));
            List<ProductResult> results = products.stream()
                    .map(productResultMapper::toResult)
                    .toList();
            ProductSearchResult.Facets facets = new ProductSearchResult.Facets(
                    hits.brandCounts(),
                    hits.categoryCounts(),
                    hits.attributeCounts(),
                    hits.priceMin() == null && hits.priceMax() == null
                            ? null
                            : new ProductSearchResult.PriceRange(hits.priceMin(), hits.priceMax()));
            return ProductSearchResult.of(results, criteria.page(), criteria.size(), hits.totalHits(), facets);
        }
        return transactionTemplate.execute(status -> ProductSearchResult.of(jpaSearchFallback(criteria)));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void syncAllToSearchIndex() {
        log.info("Syncing all published products to the catalog search index...");
        int limit = searchIndexBatchPolicy.batchSize();
        int offset = 0;
        long total = 0;
        while (true) {
            int pageOffset = offset;
            List<ProductSearchDocument> docs = transactionTemplate.execute(status ->
                    productPersistencePort.findPublished(limit, pageOffset).stream()
                            .map(this::buildSearchDocument)
                            .toList());
            if (docs == null || docs.isEmpty()) {
                break;
            }
            catalogSearchIndex.indexAll(docs);
            total += docs.size();
            offset += limit;
        }
        log.info("Synced {} published products to the catalog search index", total);
    }

    private ProductSearchDocument buildSearchDocument(Product product) {
        Map<String, String> filterable = new LinkedHashMap<>();
        if (!product.attributes().isEmpty()) {
            for (String categoryId : product.categoryIds()) {
                Optional<AttributeTemplate> template = attributeTemplatePersistencePort.findByCategoryId(categoryId);
                template.ifPresent(t -> {
                    for (Map.Entry<String, AttributeTemplate.AttributeDefinition> def : t.snapshot().entrySet()) {
                        if (def.getValue().filterable()) {
                            String value = product.attributes().get(def.getKey());
                            if (value != null) {
                                filterable.put(def.getKey(), value);
                            }
                        }
                    }
                });
            }
        }
        double rating = productReviewPersistencePort.getAverageRating(product.getProductId());
        long soldCount = productSoldCounterPersistencePort.getSoldCount(product.getProductId());
        return searchDocumentMapper.toSearchDocument(product, filterable, rating, soldCount);
    }

    private PageResult<ProductResult> jpaSearchFallback(ProductSearchCriteria criteria) {
        var page = productPersistencePort.searchFallback(
                new ProductPersistencePort.FallbackFilter(
                        criteria.q(), criteria.merchantId(), criteria.status(), criteria.brandIds(),
                        criteria.categoryIds(), criteria.priceMin(), criteria.priceMax(), criteria.attributes(),
                        ProductPersistencePort.FallbackSort.valueOf(criteria.sort().name()), criteria.ratingMin(),
                        criteria.onSale()),
                OffsetPagination.of(criteria.page(), criteria.size()));
        List<ProductResult> results = page.content().stream()
                .map(productResultMapper::toResult)
                .toList();
        return new PageResult<>(results, criteria.page(), criteria.size(), page.totalElements());
    }

    private static boolean isRealUser(String userId) {
        return userId != null && !userId.isBlank() && !userId.equals("anonymousUser");
    }

    private Product required(String productId) {
        return productPersistencePort.findById(productId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
    }

    private Product ownedBy(String productId, String merchantId) {
        Product product = required(productId);
        product.ensureOwnedBy(merchantId);
        return product;
    }

    private void requireActiveMerchant(String merchantId) {
        var merchant = merchantPersistencePort.findById(merchantId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.MERCHANT_NOT_FOUND));
        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new CatalogException(CatalogErrorCode.MERCHANT_FORBIDDEN,
                    "Merchant is not active");
        }
    }
}
