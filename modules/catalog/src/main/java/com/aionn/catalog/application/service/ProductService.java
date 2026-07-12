package com.aionn.catalog.application.service;

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
import com.aionn.catalog.application.policy.CatalogProductPolicy;
import com.aionn.catalog.application.port.out.attribute.AttributeTemplatePersistencePort;
import com.aionn.catalog.application.port.out.search.ProductSearchIndex;
import com.aionn.catalog.domain.model.AttributeTemplate;
import com.aionn.catalog.application.port.out.brand.BrandPersistencePort;
import com.aionn.catalog.application.port.out.category.CategoryPersistencePort;
import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.application.port.out.product.UserBrowsingHistoryPersistencePort;
import com.aionn.catalog.application.port.out.search.ProductSearchIndexPort;
import com.aionn.catalog.domain.model.UserBrowsingHistory;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Brand;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.valueobject.BrandStatus;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    private final ProductPersistencePort productRepository;
    private final UserBrowsingHistoryPersistencePort userBrowsingHistoryRepository;
    private final MerchantPersistencePort merchantRepository;
    private final BrandPersistencePort brandRepository;
    private final CategoryPersistencePort categoryRepository;
    private final ProductSearchIndexPort searchIndex;
    private final ProductSearchIndex catalogSearchIndex;
    private final ProductSearchDocumentMapper searchDocumentMapper;
    private final AttributeTemplatePersistencePort attributeTemplateRepository;
    private final ProductResultMapper productResultMapper;
    private final CatalogProductPolicy productPolicy;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public ProductResult create(CreateProductCommand command) {
        merchantRepository.findById(command.merchantId())
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.MERCHANT_NOT_FOUND));
        Product product = Product.create(IdGenerator.ulid(), command.merchantId(), command.name(), clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult clone(CloneProductCommand command) {
        Product source = ownedBy(command.productId(), command.merchantId());
        Product clone = Product.create(IdGenerator.ulid(), command.merchantId(), source.getName(), clock);
        Product saved = productRepository.save(clone);
        eventPublisher.publish(clone.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult defineVariant(DefineVariantCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        Money price = Money.of(command.price(), command.currency());
        product.defineVariant(command.skuId(), command.attributeValues(), price, clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult removeVariant(com.aionn.catalog.application.dto.product.command.RemoveVariantCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.removeVariant(command.skuId(), clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult updateMedia(com.aionn.catalog.application.dto.product.command.UpdateMediaCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.updateMedia(command.images(), clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult updateAiMetadata(
            com.aionn.catalog.application.dto.product.command.UpdateAiMetadataCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.updateAiMetadata(command.tags(), command.aiDescription(), clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult assignCollections(
            com.aionn.catalog.application.dto.product.command.AssignCollectionsCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.assignToCollections(command.collectionIds(), clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult emergencyTakedown(
            com.aionn.catalog.application.dto.product.command.EmergencyTakedownCommand command) {
        Product product = required(command.productId());
        product.emergencyTakedown(command.adminId(), command.reason(), clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        searchIndex.delete(product.getProductId());
        return productResultMapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public PageResult<ProductResult> search(String keyword, OffsetPagination pagination) {
        List<String> ids = searchIndex.searchIds(keyword, pagination);
        List<Product> products = productRepository.findAllByIds(ids);
        List<ProductResult> content = products.stream()
                .map(productResultMapper::toResult)
                .toList();
        long total = searchIndex.countMatches(keyword);
        return new PageResult<>(content, pagination.page(), pagination.size(), total);
    }

    public ProductResult changeVariantPrice(ChangeVariantPriceCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.changeVariantPrice(command.skuId(), Money.of(command.newPrice(), command.currency()), clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public BulkPriceUpdateResult bulkPriceUpdate(BulkPriceUpdateCommand command) {
        if (command.items() == null || command.items().isEmpty()) {
            return new BulkPriceUpdateResult(0, 0, List.of());
        }
        int max = productPolicy.getBulkPriceUpdateMaxSize();
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
                productRepository.save(product);
                eventPublisher.publish(product.pullEvents());
                updated++;
            } catch (CatalogException e) {
                failed.add(item.skuId());
                skipped++;
            }
        }
        return new BulkPriceUpdateResult(updated, skipped, failed);
    }

    public ProductResult assignBrand(AssignBrandCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        Brand brand = brandRepository.findById(command.brandId())
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.BRAND_NOT_FOUND));
        if (brand.getStatus() != BrandStatus.ACTIVE) {
            throw new CatalogException(CatalogErrorCode.PRODUCT_BRAND_NOT_APPROVED);
        }
        product.assignBrand(command.brandId(), clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult categorize(AssignCategoriesCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        if (command.categoryIds() == null || command.categoryIds().isEmpty()) {
            throw new CatalogException(CatalogErrorCode.PRODUCT_CATEGORY_REQUIRED);
        }
        for (String categoryId : command.categoryIds()) {
            categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new CatalogException(CatalogErrorCode.CATEGORY_NOT_FOUND,
                            "Category not found: " + categoryId));
        }
        product.categorize(command.categoryIds(), clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult defineAttributes(DefineAttributesCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.defineAttributes(command.attributes(), clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult publish(PublishProductCommand command) {
        Product product = required(command.productId());
        product.publish(command.adminId(), clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        searchIndex.index(saved);
        return productResultMapper.toResult(saved);
    }

    public ProductResult submitForReview(SubmitForReviewCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.submitForReview(command.merchantId(), clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult reject(RejectProductCommand command) {
        Product product = required(command.productId());
        product.reject(command.adminId(), command.reasonCode(), command.feedback(), clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult deactivate(DeactivateProductCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.deactivate(command.reason(), clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        searchIndex.delete(product.getProductId());
        return productResultMapper.toResult(saved);
    }

    public ProductResult restore(RestoreProductCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.restore(clock);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        searchIndex.index(saved);
        return productResultMapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public ProductResult get(String productId) {
        return productResultMapper.toResult(required(productId));
    }

    @Transactional(readOnly = true)
    public List<ProductResult> getBySkuIds(List<String> skuIds) {
        return productRepository.findAllBySkuIds(skuIds).stream()
                .map(productResultMapper::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<ProductResult> listByMerchant(String merchantId, OffsetPagination pagination) {
        List<ProductResult> content = productRepository.listByMerchant(merchantId, pagination).stream()
                .map(productResultMapper::toResult)
                .toList();
        long total = productRepository.countByMerchant(merchantId);
        return new PageResult<>(content, pagination.page(), pagination.size(), total);
    }

    @Transactional(readOnly = true)
    public PageResult<ProductResult> listByStatus(ProductStatus status, OffsetPagination pagination) {
        List<ProductResult> content = productRepository.listByStatus(status, pagination).stream()
                .map(productResultMapper::toResult)
                .toList();
        long total = productRepository.countByStatus(status);
        return new PageResult<>(content, pagination.page(), pagination.size(), total);
    }

    @Transactional(readOnly = true)
    public List<ProductResult> getRelatedProducts(String productId, int limit) {
        Product product = required(productId);
        return productRepository.findRelatedProducts(
                product.getProductId(),
                product.getBrandId(),
                product.categoryIds(),
                limit).stream()
                .map(productResultMapper::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResult> getPopularProducts(int limit) {
        return popularProducts(limit);
    }

    private List<ProductResult> popularProducts(int limit) {
        return productRepository.findPopularProducts(limit).stream()
                .map(productResultMapper::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResult> getPersonalizedProducts(String userId, List<String> categoryIds, List<String> brandIds,
            int limit) {
        List<String> activeCategoryIds = categoryIds != null ? categoryIds : List.of();
        List<String> activeBrandIds = brandIds != null ? brandIds : List.of();

        if (isRealUser(userId)) {
            Optional<UserBrowsingHistory> history = userBrowsingHistoryRepository.findByUserId(userId);
            if (history.isPresent()) {
                activeCategoryIds = history.get().getCategoryIds();
                activeBrandIds = history.get().getBrandIds();
            }
        }

        if (activeCategoryIds.isEmpty() && activeBrandIds.isEmpty()) {
            return popularProducts(limit);
        }

        List<Product> products = productRepository.findPersonalizedProducts(activeCategoryIds, activeBrandIds, limit);
        if (products.isEmpty()) {
            return popularProducts(limit);
        }
        return products.stream()
                .map(productResultMapper::toResult)
                .toList();
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void trackProductView(String productId, String userId) {
        if (!isRealUser(userId)) {
            return;
        }
        Product product = required(productId);
        UserBrowsingHistory history = userBrowsingHistoryRepository.findByUserId(userId)
                .orElseGet(() -> UserBrowsingHistory.create(userId));
        history.trackView(product.categoryIds(), product.getBrandId());
        userBrowsingHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public ProductSearchResult searchCatalog(ProductSearchCriteria criteria) {
        Optional<ProductSearchIndex.SearchHits> hitsOpt = catalogSearchIndex.search(criteria);
        if (hitsOpt.isPresent()) {
            ProductSearchIndex.SearchHits hits = hitsOpt.get();
            List<Product> products = productRepository.findByIdsPreserveOrder(hits.productIds());
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
        return ProductSearchResult.of(jpaSearchFallback(criteria));
    }

    @Transactional(readOnly = true)
    public void syncAllToSearchIndex() {
        log.info("Syncing all published products to the catalog search index...");
        int limit = 100;
        int offset = 0;
        long total = 0;
        while (true) {
            List<Product> products = productRepository.findPublished(limit, offset);
            if (products.isEmpty()) {
                break;
            }
            List<ProductSearchDocument> docs = products.stream()
                    .map(this::buildSearchDocument)
                    .toList();
            catalogSearchIndex.indexAll(docs);
            total += products.size();
            offset += limit;
        }
        log.info("Synced {} published products to the catalog search index", total);
    }

    private ProductSearchDocument buildSearchDocument(Product product) {
        Map<String, String> filterable = new LinkedHashMap<>();
        if (!product.attributes().isEmpty()) {
            for (String categoryId : product.categoryIds()) {
                Optional<AttributeTemplate> template = attributeTemplateRepository.findByCategoryId(categoryId);
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
        return searchDocumentMapper.toSearchDocument(product, filterable);
    }

    private PageResult<ProductResult> jpaSearchFallback(ProductSearchCriteria criteria) {
        ProductStatus requiredStatus = criteria.status() != null ? criteria.status() : ProductStatus.PUBLISHED;
        List<Product> filtered = fallbackCandidates(criteria).stream()
                .filter(p -> p.getStatus() == requiredStatus)
                .filter(p -> matchesFacets(p, criteria))
                .toList();

        long totalElements = filtered.size();
        int from = Math.min(criteria.page() * criteria.size(), filtered.size());
        int to = Math.min(from + criteria.size(), filtered.size());
        List<ProductResult> results = filtered.subList(from, to).stream()
                .map(productResultMapper::toResult)
                .toList();
        return new PageResult<>(results, criteria.page(), criteria.size(), totalElements);
    }

    private List<Product> fallbackCandidates(ProductSearchCriteria criteria) {
        if (criteria.merchantId() != null && !criteria.merchantId().isBlank()) {
            return productRepository.listByMerchant(criteria.merchantId(),
                    OffsetPagination.of(0, OffsetPagination.MAX_SIZE));
        }
        if (criteria.hasText()) {
            return productRepository.searchPublished(criteria.q(), 10_000, 0);
        }
        return productRepository.findPublished(10_000, 0);
    }

    private static boolean matchesFacets(Product product, ProductSearchCriteria criteria) {
        if (!criteria.brandIds().isEmpty()
                && (product.getBrandId() == null || !criteria.brandIds().contains(product.getBrandId()))) {
            return false;
        }
        if (!criteria.categoryIds().isEmpty()
                && product.categoryIds().stream().noneMatch(criteria.categoryIds()::contains)) {
            return false;
        }
        return matchesPrice(product, criteria.priceMin(), criteria.priceMax());
    }

    private static boolean matchesPrice(Product product, BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return true;
        }
        return product.variants().stream().anyMatch(v -> {
            BigDecimal price = v.price() == null ? null : v.price().amount();
            if (price == null) {
                return false;
            }
            if (min != null && price.compareTo(min) < 0) {
                return false;
            }
            return max == null || price.compareTo(max) <= 0;
        });
    }

    private static boolean isRealUser(String userId) {
        return userId != null && !userId.isBlank() && !userId.equals("anonymousUser");
    }

    private Product required(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
    }

    private Product ownedBy(String productId, String merchantId) {
        Product product = required(productId);
        product.ensureOwnedBy(merchantId);
        return product;
    }
}
