package com.aionn.catalog.application.service;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.command.AssignBrandCommand;
import com.aionn.catalog.application.dto.product.command.AssignCategoriesCommand;
import com.aionn.catalog.application.dto.product.command.BulkPriceUpdateCommand;
import com.aionn.catalog.application.dto.product.command.ChangeVariantPriceCommand;
import com.aionn.catalog.application.dto.product.command.CreateProductCommand;
import com.aionn.catalog.application.dto.product.command.DeactivateProductCommand;
import com.aionn.catalog.application.dto.product.command.DefineVariantCommand;
import com.aionn.catalog.application.dto.product.command.PublishProductCommand;
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
import com.aionn.catalog.domain.model.Category;
import com.aionn.catalog.domain.model.Merchant;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
    private static final String MERCHANT_ID = "01HZMER0000000000000000001";
    private static final String BRAND_ID = "01HZBRD0000000000000000001";
    private static final String CATEGORY_ID = "01HZCAT0000000000000000001";
    private static final String ADMIN_ID = "01HZADM0000000000000000001";

    @Mock
    private ProductPersistencePort productRepository;
    @Mock
    private MerchantPersistencePort merchantRepository;
    @Mock
    private BrandPersistencePort brandRepository;
    @Mock
    private CategoryPersistencePort categoryRepository;
    @Mock
    private ProductSearchIndexPort searchIndex;
    @Mock
    private ProductResultMapper productResultMapper;
    @Mock
    private CatalogProductPolicy productPolicy;
    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ProductService productService;

    private ProductResult sampleResult;

    @BeforeEach
    void setUp() {
        sampleResult = new ProductResult(
                PRODUCT_ID, MERCHANT_ID, "Widget", null,
                List.of(), List.of(), List.of(), Map.of(),
                List.of(), null, "DRAFT",
                Instant.now(), Instant.now());
    }

    private Product publishableProduct() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.categorize(List.of(CATEGORY_ID));
        product.defineVariant("sku-1", Map.of("color", "red"),
                Money.of(new BigDecimal("10.00"), "VND"));
        product.pullEvents();
        return product;
    }

    @Test
    void createRequiresExistingMerchant() {
        when(merchantRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(new CreateProductCommand("missing", "Widget")))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.MERCHANT_NOT_FOUND.getCode());

        verify(productRepository, never()).save(any());
    }

    @Test
    void createPersistsAndPublishes() {
        Merchant merchant = Merchant.register(MERCHANT_ID, "owner-1", "Acme", new BigDecimal("0.05"));
        when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productResultMapper.toResult(any(Product.class))).thenReturn(sampleResult);

        ProductResult result = productService.create(new CreateProductCommand(MERCHANT_ID, "Widget"));

        assertThat(result).isEqualTo(sampleResult);
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void defineVariantAddsSku() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.pullEvents();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productResultMapper.toResult(any(Product.class))).thenReturn(sampleResult);

        productService.defineVariant(new DefineVariantCommand(PRODUCT_ID, MERCHANT_ID, "sku-1",
                Map.of("color", "red"), new BigDecimal("10.00"), "VND"));

        assertThat(product.variants()).hasSize(1);
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void defineVariantRejectsWhenNotOwner() {
        Product product = Product.create(PRODUCT_ID, "other-merchant", "Widget");
        product.pullEvents();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.defineVariant(new DefineVariantCommand(
                PRODUCT_ID, MERCHANT_ID, "sku-1", Map.of("color", "red"),
                new BigDecimal("10.00"), "VND")))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_FORBIDDEN.getCode());
    }

    @Test
    void changeVariantPriceUpdatesSku() {
        Product product = publishableProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productResultMapper.toResult(any(Product.class))).thenReturn(sampleResult);

        productService.changeVariantPrice(new ChangeVariantPriceCommand(
                PRODUCT_ID, MERCHANT_ID, "sku-1", new BigDecimal("20.00"), "VND"));

        assertThat(product.findVariant("sku-1").orElseThrow().price().amount())
                .isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    void bulkPriceUpdateRejectsOverBatchSize() {
        when(productPolicy.getBulkPriceUpdateMaxSize()).thenReturn(2);
        List<BulkPriceUpdateCommand.Item> items = List.of(
                new BulkPriceUpdateCommand.Item(PRODUCT_ID, "sku-1", new BigDecimal("1"), "VND"),
                new BulkPriceUpdateCommand.Item(PRODUCT_ID, "sku-2", new BigDecimal("1"), "VND"),
                new BulkPriceUpdateCommand.Item(PRODUCT_ID, "sku-3", new BigDecimal("1"), "VND"));

        assertThatThrownBy(() -> productService.bulkPriceUpdate(new BulkPriceUpdateCommand(MERCHANT_ID, items)))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_BULK_TOO_LARGE.getCode());
    }

    @Test
    void bulkPriceUpdateReturnsEmptyResultWhenNoItems() {
        BulkPriceUpdateResult result = productService.bulkPriceUpdate(
                new BulkPriceUpdateCommand(MERCHANT_ID, List.of()));

        assertThat(result.updated()).isZero();
        assertThat(result.skipped()).isZero();
    }

    @Test
    void bulkPriceUpdateSkipsFailedItemsAndCountsSuccesses() {
        when(productPolicy.getBulkPriceUpdateMaxSize()).thenReturn(10);
        Product product = publishableProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.findById("missing")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        List<BulkPriceUpdateCommand.Item> items = List.of(
                new BulkPriceUpdateCommand.Item(PRODUCT_ID, "sku-1", new BigDecimal("20"), "VND"),
                new BulkPriceUpdateCommand.Item("missing", "sku-2", new BigDecimal("30"), "VND"));

        BulkPriceUpdateResult result = productService.bulkPriceUpdate(
                new BulkPriceUpdateCommand(MERCHANT_ID, items));

        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failedSkus()).containsExactly("sku-2");
    }

    @Test
    void assignBrandRequiresActiveBrand() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.pullEvents();
        Brand brand = Brand.create(BRAND_ID, "Acme", null, null);
        brand.softDelete("gone");
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(brandRepository.findById(BRAND_ID)).thenReturn(Optional.of(brand));

        assertThatThrownBy(() -> productService.assignBrand(
                new AssignBrandCommand(PRODUCT_ID, MERCHANT_ID, BRAND_ID)))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_BRAND_NOT_APPROVED.getCode());
    }

    @Test
    void assignBrandSetsBrandIdWhenActive() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.pullEvents();
        Brand brand = Brand.create(BRAND_ID, "Acme", null, null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(brandRepository.findById(BRAND_ID)).thenReturn(Optional.of(brand));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productResultMapper.toResult(any(Product.class))).thenReturn(sampleResult);

        productService.assignBrand(new AssignBrandCommand(PRODUCT_ID, MERCHANT_ID, BRAND_ID));

        assertThat(product.getBrandId()).isEqualTo(BRAND_ID);
    }

    @Test
    void categorizeRequiresAllCategoriesToExist() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.pullEvents();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(categoryRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.categorize(new AssignCategoriesCommand(
                PRODUCT_ID, MERCHANT_ID, List.of("missing"))))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.CATEGORY_NOT_FOUND.getCode());
    }

    @Test
    void categorizeRejectsEmptyList() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.pullEvents();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.categorize(new AssignCategoriesCommand(
                PRODUCT_ID, MERCHANT_ID, List.of())))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_CATEGORY_REQUIRED.getCode());
    }

    @Test
    void categorizeAppliesCategoriesWhenAllExist() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.pullEvents();
        Category category = Category.create(CATEGORY_ID, null, "A", "a");
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productResultMapper.toResult(any(Product.class))).thenReturn(sampleResult);

        productService.categorize(new AssignCategoriesCommand(
                PRODUCT_ID, MERCHANT_ID, List.of(CATEGORY_ID)));

        assertThat(product.categoryIds()).containsExactly(CATEGORY_ID);
    }

    @Test
    void publishTransitionsAndIndexes() {
        Product product = publishableProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productResultMapper.toResult(any(Product.class))).thenReturn(sampleResult);

        productService.publish(new PublishProductCommand(PRODUCT_ID, ADMIN_ID));

        assertThat(product.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
        verify(searchIndex).index(product);
    }

    @Test
    void deactivateHidesAndRemovesFromIndex() {
        Product product = publishableProduct();
        product.publish(ADMIN_ID);
        product.pullEvents();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productResultMapper.toResult(any(Product.class))).thenReturn(sampleResult);

        productService.deactivate(new DeactivateProductCommand(PRODUCT_ID, MERCHANT_ID, "policy"));

        assertThat(product.getStatus()).isEqualTo(ProductStatus.HIDDEN);
        verify(searchIndex).delete(PRODUCT_ID);
    }

    @Test
    void getThrowsWhenProductMissing() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.get("missing"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_NOT_FOUND.getCode());
    }

    @Test
    void listByMerchantReturnsPageResult() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        when(productRepository.listByMerchant(MERCHANT_ID, OffsetPagination.of(0, 20)))
                .thenReturn(List.of(product));
        when(productRepository.countByMerchant(MERCHANT_ID)).thenReturn(1L);
        when(productResultMapper.toResult(product)).thenReturn(sampleResult);

        PageResult<ProductResult> page = productService.listByMerchant(MERCHANT_ID, OffsetPagination.of(0, 20));

        assertThat(page.content()).containsExactly(sampleResult);
        assertThat(page.totalElements()).isEqualTo(1L);
    }

    @Test
    void listByStatusReturnsPageResult() {
        Product product = publishableProduct();
        product.publish(ADMIN_ID);
        when(productRepository.listByStatus(ProductStatus.PUBLISHED, OffsetPagination.of(0, 20)))
                .thenReturn(List.of(product));
        when(productRepository.countByStatus(ProductStatus.PUBLISHED)).thenReturn(1L);
        when(productResultMapper.toResult(product)).thenReturn(sampleResult);

        PageResult<ProductResult> page = productService.listByStatus(ProductStatus.PUBLISHED,
                OffsetPagination.of(0, 20));

        assertThat(page.content()).containsExactly(sampleResult);
    }

    @Test
    void removeVariantDelegatesToDomain() {
        Product product = publishableProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productResultMapper.toResult(any(Product.class))).thenReturn(sampleResult);

        productService.removeVariant(new com.aionn.catalog.application.dto.product.command.RemoveVariantCommand(
                PRODUCT_ID, MERCHANT_ID, "sku-1"));

        assertThat(product.variants()).isEmpty();
    }

    @Test
    void updateMediaReplacesImages() {
        Product product = publishableProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productResultMapper.toResult(any(Product.class))).thenReturn(sampleResult);

        productService.updateMedia(new com.aionn.catalog.application.dto.product.command.UpdateMediaCommand(
                PRODUCT_ID, MERCHANT_ID, List.of("img1", "img2")));

        assertThat(product.imageList()).containsExactly("img1", "img2");
    }

    @Test
    void updateAiMetadataAppliesTagsAndDescription() {
        Product product = publishableProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productResultMapper.toResult(any(Product.class))).thenReturn(sampleResult);

        productService.updateAiMetadata(new com.aionn.catalog.application.dto.product.command.UpdateAiMetadataCommand(
                PRODUCT_ID, MERCHANT_ID, List.of("premium"), "handmade widget"));

        assertThat(product.tags()).containsExactly("premium");
        assertThat(product.getAiDescription()).isEqualTo("handmade widget");
    }

    @Test
    void assignCollectionsReplacesList() {
        Product product = publishableProduct();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productResultMapper.toResult(any(Product.class))).thenReturn(sampleResult);

        productService.assignCollections(new com.aionn.catalog.application.dto.product.command.AssignCollectionsCommand(
                PRODUCT_ID, MERCHANT_ID, List.of("coll-1", "coll-2")));

        assertThat(product.collectionIds()).containsExactly("coll-1", "coll-2");
    }

    @Test
    void emergencyTakedownRemovesFromSearchIndex() {
        Product product = publishableProduct();
        product.publish(ADMIN_ID);
        product.pullEvents();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productResultMapper.toResult(any(Product.class))).thenReturn(sampleResult);

        productService.emergencyTakedown(
                new com.aionn.catalog.application.dto.product.command.EmergencyTakedownCommand(
                        PRODUCT_ID, ADMIN_ID, "policy"));

        assertThat(product.getStatus()).isEqualTo(ProductStatus.TAKEN_DOWN);
        verify(searchIndex).delete(PRODUCT_ID);
    }

    @Test
    void searchDelegatesToIndex() {
        when(searchIndex.searchIds("keyword", OffsetPagination.of(0, 20))).thenReturn(List.of(PRODUCT_ID));
        Product product = publishableProduct();
        when(productRepository.findAllByIds(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(searchIndex.countMatches("keyword")).thenReturn(1L);
        when(productResultMapper.toResult(product)).thenReturn(sampleResult);

        PageResult<ProductResult> page = productService.search("keyword", OffsetPagination.of(0, 20));

        assertThat(page.content()).containsExactly(sampleResult);
        assertThat(page.totalElements()).isEqualTo(1L);
    }

    @Test
    void searchReturnsEmptyWhenIndexEmpty() {
        when(searchIndex.searchIds("nothing", OffsetPagination.of(0, 20))).thenReturn(List.of());
        when(productRepository.findAllByIds(List.of())).thenReturn(List.of());
        when(searchIndex.countMatches("nothing")).thenReturn(0L);

        PageResult<ProductResult> page = productService.search("nothing", OffsetPagination.of(0, 20));

        assertThat(page.content()).isEmpty();
    }
}
