package com.aionn.catalog.application.service;

import com.aionn.catalog.application.port.out.merchant.StockAlertPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.contracts.event.catalog.ProductBackInStockIntegrationEvent;
import com.aionn.contracts.publisher.IntegrationEventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAlertService {

    private final StockAlertPersistencePort alertRepository;
    private final ProductPersistencePort productRepository;
    private final IntegrationEventPublisher integrationEventPublisher;
    private final Clock clock;

    @Transactional
    public void subscribe(String userId, String productId, String skuId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new CatalogException(CatalogErrorCode.PRODUCT_INVALID_TRANSITION,
                    "Stock alerts are only available for published products");
        }
        boolean ownsSku = product.variants().stream()
                .anyMatch(variant -> variant.skuId().equals(skuId));
        if (!ownsSku) {
            throw new CatalogException(CatalogErrorCode.PRODUCT_VARIANT_NOT_FOUND);
        }
        alertRepository.subscribe(IdGenerator.ulid(), userId, productId, skuId, clock.instant());
    }

    @Transactional
    public void unsubscribe(String userId, String skuId) {
        alertRepository.unsubscribe(userId, skuId);
    }

    @Transactional(readOnly = true)
    public boolean isSubscribed(String userId, String skuId) {
        return alertRepository.exists(userId, skuId);
    }

    /**
     * Called by the inventory integration listener when stock is replenished. Pending alerts are
     * claimed by timestamp so a duplicate replenishment event cannot notify twice.
     */
    @Transactional
    public void notifyRestocked(String skuId, Instant occurredAt) {
        var pending = alertRepository.findPendingBySku(skuId);
        if (pending.isEmpty()) {
            return;
        }
        for (var alert : pending) {
            String productName = productRepository.findById(alert.productId())
                    .map(Product::getName)
                    .orElse(null);
            alertRepository.markNotified(alert.alertId(), clock.instant());
            integrationEventPublisher.publish(new ProductBackInStockIntegrationEvent(
                    IdGenerator.ulid(),
                    alert.userId(),
                    alert.productId(),
                    alert.skuId(),
                    productName,
                    occurredAt));
        }
        log.info("Published {} back-in-stock notifications for sku {}", pending.size(), skuId);
    }
}
