package com.aionn.catalog.infrastructure.listener;

import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.domain.event.ProductEvents;
import com.aionn.catalog.domain.model.Merchant;
import com.aionn.catalog.domain.model.Product;
import com.aionn.contracts.event.catalog.MerchantFollowerNotificationIntegrationEvent;
import com.aionn.contracts.publisher.IntegrationEventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MerchantFollowerProductListener {

    private static final String TYPE_PRODUCT_PUBLISHED = "PRODUCT_PUBLISHED";

    private final ProductPersistencePort productPersistencePort;
    private final MerchantPersistencePort merchantPersistencePort;
    private final IntegrationEventPublisher integrationEventPublisher;

    @EventListener
    public void onProductPublished(ProductEvents.ProductPublished event) {
        Product product = productPersistencePort.findById(event.productId()).orElse(null);
        if (product == null) {
            return;
        }
        Merchant merchant = merchantPersistencePort.findById(product.getMerchantId()).orElse(null);
        if (merchant == null) {
            return;
        }
        integrationEventPublisher.publish(new MerchantFollowerNotificationIntegrationEvent(
                IdGenerator.ulid(),
                merchant.getMerchantId(),
                TYPE_PRODUCT_PUBLISHED,
                "Sản phẩm mới từ " + merchant.getName(),
                product.getName(),
                "/products/" + product.getProductId(),
                event.occurredAt()));
    }
}
