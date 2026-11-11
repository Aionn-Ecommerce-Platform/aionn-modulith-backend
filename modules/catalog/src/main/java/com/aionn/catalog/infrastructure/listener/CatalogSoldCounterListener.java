package com.aionn.catalog.infrastructure.listener;

import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductSoldCounterPersistencePort;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.catalog.infrastructure.search.ProductSearchIndexUpdater;
import com.aionn.contracts.event.ordering.OrderCompletedIntegrationEvent;
import com.aionn.contracts.port.ordering.OrderSnapshotQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogSoldCounterListener {

    private final OrderSnapshotQueryPort orderSnapshotQueryPort;
    private final ProductPersistencePort productRepository;
    private final ProductSoldCounterPersistencePort soldCounterRepository;
    private final ProductSearchIndexUpdater searchIndexUpdater;

    @EventListener
    public void onOrderCompleted(OrderCompletedIntegrationEvent event) {
        orderSnapshotQueryPort.findOrderById(event.orderId()).ifPresent(snapshot -> {
            if (snapshot.lines() == null || snapshot.lines().isEmpty()) {
                return;
            }
            Map<String, Integer> quantityBySku = new LinkedHashMap<>();
            for (OrderSnapshotQueryPort.OrderSnapshot.Line line : snapshot.lines()) {
                quantityBySku.merge(line.skuId(), line.qty(), Integer::sum);
            }

            List<Product> products = productRepository.findAllBySkuIds(quantityBySku.keySet());
            Map<String, Long> quantityByProduct = new LinkedHashMap<>();
            for (Product product : products) {
                for (ProductVariant variant : product.variants()) {
                    Integer qty = quantityBySku.get(variant.skuId());
                    if (qty != null) {
                        quantityByProduct.merge(product.getProductId(), qty.longValue(), Long::sum);
                    }
                }
            }

            quantityByProduct.forEach((productId, qty) -> {
                soldCounterRepository.incrementSoldCount(productId, qty);
                searchIndexUpdater.reindexProduct(productId);
                log.debug("Incremented sold counter for product {} by {}", productId, qty);
            });
        });
    }
}
