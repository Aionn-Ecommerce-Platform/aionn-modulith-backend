package com.aionn.catalog.infrastructure.integration.merchant;

import com.aionn.catalog.domain.event.MerchantEvents;
import com.aionn.sharedkernel.integration.event.catalog.MerchantActivatedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.catalog.MerchantClosedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.catalog.MerchantSuspendedIntegrationEvent;
import com.aionn.sharedkernel.util.IdGenerator;
import org.springframework.stereotype.Component;

@Component
public class MerchantIntegrationEventMapper {

    public MerchantSuspendedIntegrationEvent toIntegrationEvent(MerchantEvents.MerchantSuspended domainEvent) {
        return new MerchantSuspendedIntegrationEvent(
                IdGenerator.ulid(),
                domainEvent.merchantId(),
                domainEvent.reason(),
                domainEvent.occurredAt());
    }

    public MerchantClosedIntegrationEvent toIntegrationEvent(MerchantEvents.MerchantClosed domainEvent) {
        return new MerchantClosedIntegrationEvent(
                IdGenerator.ulid(),
                domainEvent.merchantId(),
                domainEvent.reason(),
                domainEvent.occurredAt());
    }

    public MerchantActivatedIntegrationEvent toIntegrationEvent(MerchantEvents.MerchantActivated domainEvent) {
        return new MerchantActivatedIntegrationEvent(
                IdGenerator.ulid(),
                domainEvent.merchantId(),
                domainEvent.adminId(),
                domainEvent.occurredAt());
    }
}
