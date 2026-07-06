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
import com.aionn.catalog.application.mapper.ProductResultMapper;
import com.aionn.catalog.application.policy.CatalogProductPolicy;
import com.aionn.catalog.application.port.out.brand.BrandPersistencePort;
import com.aionn.catalog.application.port.out.category.CategoryPersistencePort;
import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.application.port.out.search.ProductSearchIndexPort;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductPersistencePort productRepository;
    private final MerchantPersistencePort merchantRepository;
    private final BrandPersistencePort brandRepository;
    private final CategoryPersistencePort categoryRepository;
    private final ProductSearchIndexPort searchIndex;
    private final ProductResultMapper productResultMapper;
    private final CatalogProductPolicy productPolicy;
    private final EventPublisher eventPublisher;

    public ProductResult create(CreateProductCommand command) {
        merchantRepository.findById(command.merchantId())
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.MERCHANT_NOT_FOUND));
        Product product = Product.create(IdGenerator.ulid(), command.merchantId(), command.name());
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult clone(CloneProductCommand command) {
        Product source = ownedBy(command.productId(), command.merchantId());
        Product clone = Product.create(IdGenerator.ulid(), command.merchantId(), source.getName());
        Product saved = productRepository.save(clone);
        eventPublisher.publish(clone.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult defineVariant(DefineVariantCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        Money price = Money.of(command.price(), command.currency());
        product.defineVariant(command.skuId(), command.attributeValues(), price);
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult removeVariant(com.aionn.catalog.application.dto.product.command.RemoveVariantCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.removeVariant(command.skuId());
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult updateMedia(com.aionn.catalog.application.dto.product.command.UpdateMediaCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.updateMedia(command.images());
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult updateAiMetadata(
            com.aionn.catalog.application.dto.product.command.UpdateAiMetadataCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.updateAiMetadata(command.tags(), command.aiDescription());
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult assignCollections(
            com.aionn.catalog.application.dto.product.command.AssignCollectionsCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.assignToCollections(command.collectionIds());
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult emergencyTakedown(
            com.aionn.catalog.application.dto.product.command.EmergencyTakedownCommand command) {
        Product product = required(command.productId());
        product.emergencyTakedown(command.adminId(), command.reason());
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
        product.changeVariantPrice(command.skuId(), Money.of(command.newPrice(), command.currency()));
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
                product.changeVariantPrice(item.skuId(), Money.of(item.newPrice(), item.currency()));
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
        product.assignBrand(command.brandId());
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
        product.categorize(command.categoryIds());
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult defineAttributes(DefineAttributesCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.defineAttributes(command.attributes());
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult publish(PublishProductCommand command) {
        Product product = required(command.productId());
        product.publish(command.adminId());
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        searchIndex.index(saved);
        return productResultMapper.toResult(saved);
    }

    public ProductResult submitForReview(SubmitForReviewCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.submitForReview(command.merchantId());
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult reject(RejectProductCommand command) {
        Product product = required(command.productId());
        product.reject(command.adminId(), command.reasonCode(), command.feedback());
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        return productResultMapper.toResult(saved);
    }

    public ProductResult deactivate(DeactivateProductCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.deactivate(command.reason());
        Product saved = productRepository.save(product);
        eventPublisher.publish(product.pullEvents());
        searchIndex.delete(product.getProductId());
        return productResultMapper.toResult(saved);
    }

    public ProductResult restore(RestoreProductCommand command) {
        Product product = ownedBy(command.productId(), command.merchantId());
        product.restore();
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
