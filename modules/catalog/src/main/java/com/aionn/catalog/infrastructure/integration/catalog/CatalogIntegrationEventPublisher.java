package com.aionn.catalog.infrastructure.integration.catalog;

import com.aionn.catalog.infrastructure.integration.merchant.MerchantIntegrationEventMapper;
import com.aionn.catalog.domain.event.MerchantEvents;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogIntegrationEventPublisher {

    private final IntegrationEventPublisher integrationEventPublisher;
    private final MerchantIntegrationEventMapper mapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMerchantSuspended(MerchantEvents.MerchantSuspended event) {
        log.debug("Publishing MerchantSuspendedIntegrationEvent for merchant: {}", event.merchantId());
        var integrationEvent = mapper.toIntegrationEvent(event);
        integrationEventPublisher.publish(integrationEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMerchantClosed(MerchantEvents.MerchantClosed event) {
        log.debug("Publishing MerchantClosedIntegrationEvent for merchant: {}", event.merchantId());
        var integrationEvent = mapper.toIntegrationEvent(event);
        integrationEventPublisher.publish(integrationEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMerchantActivated(MerchantEvents.MerchantActivated event) {
        log.debug("Publishing MerchantActivatedIntegrationEvent for merchant: {}", event.merchantId());
        var integrationEvent = mapper.toIntegrationEvent(event);
        integrationEventPublisher.publish(integrationEvent);
    }
}
