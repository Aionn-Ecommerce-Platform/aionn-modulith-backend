package com.aionn.catalog.application.service;

import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.mapper.ProductResultMapper;
import com.aionn.catalog.application.port.out.merchant.WishlistPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistPersistencePort wishlistRepository;
    private final ProductPersistencePort productRepository;
    private final ProductResultMapper productResultMapper;
    private final Clock clock;

    public void add(String userId, String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new CatalogException(CatalogErrorCode.PRODUCT_INVALID_TRANSITION,
                    "Only published products can be saved");
        }
        wishlistRepository.add(userId, productId, clock.instant());
    }

    public void remove(String userId, String productId) {
        wishlistRepository.remove(userId, productId);
    }

    @Transactional(readOnly = true)
    public boolean isWishlisted(String userId, String productId) {
        return wishlistRepository.exists(userId, productId);
    }

    @Transactional(readOnly = true)
    public WishlistPage list(String userId, int page, int size) {
        long total = wishlistRepository.count(userId);
        List<String> productIds = wishlistRepository.findProductIds(userId, page * size, size);
        if (productIds.isEmpty()) {
            return new WishlistPage(List.of(), total);
        }
        Map<String, Product> products = productRepository.findAllByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));
        List<Product> ordered = productIds.stream()
                .map(products::get)
                .filter(product -> product != null && product.getStatus() == ProductStatus.PUBLISHED)
                .toList();
        return new WishlistPage(productResultMapper.toResults(ordered), total);
    }

    public record WishlistPage(List<ProductResult> content, long totalElements) {
    }
}
