package com.aionn.catalog.infrastructure.integration;
import com.aionn.catalog.infrastructure.integration.catalog.CatalogIntegrationEventPublisher;
import com.aionn.catalog.infrastructure.integration.merchant.MerchantIntegrationEventMapper;

import com.aionn.catalog.domain.event.MerchantEvents;
import com.aionn.sharedkernel.integration.event.catalog.MerchantActivatedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.catalog.MerchantClosedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.catalog.MerchantSuspendedIntegrationEvent;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogIntegrationEventPublisherTest {

        @Mock
        private IntegrationEventPublisher integrationEventPublisher;
        @Mock
        private MerchantIntegrationEventMapper mapper;

        @InjectMocks
        private CatalogIntegrationEventPublisher publisher;

        @Test
        void publishesMerchantSuspendedIntegrationEvent() {
                Instant now = Instant.now();
                MerchantEvents.MerchantSuspended domain = new MerchantEvents.MerchantSuspended(
                                "m-1", "policy violation", "admin-1", now, now);
                MerchantSuspendedIntegrationEvent integrationEvent = new MerchantSuspendedIntegrationEvent(
                                "evt-1", "m-1", "policy violation", now);
                when(mapper.toIntegrationEvent(domain)).thenReturn(integrationEvent);

                publisher.onMerchantSuspended(domain);

                verify(integrationEventPublisher).publish(integrationEvent);
        }

        @Test
        void publishesMerchantClosedIntegrationEvent() {
                Instant now = Instant.now();
                MerchantEvents.MerchantClosed domain = new MerchantEvents.MerchantClosed(
                                "m-1", "voluntary", now, now);
                MerchantClosedIntegrationEvent integrationEvent = new MerchantClosedIntegrationEvent(
                                "evt-2", "m-1", "voluntary", now);
                when(mapper.toIntegrationEvent(domain)).thenReturn(integrationEvent);

                publisher.onMerchantClosed(domain);

                verify(integrationEventPublisher).publish(integrationEvent);
        }

        @Test
        void publishesMerchantActivatedIntegrationEvent() {
                Instant now = Instant.now();
                MerchantEvents.MerchantActivated domain = new MerchantEvents.MerchantActivated(
                                "m-1", "admin-1", "reinstate", now, now);
                MerchantActivatedIntegrationEvent integrationEvent = new MerchantActivatedIntegrationEvent(
                                "evt-3", "m-1", "admin-1", now);
                when(mapper.toIntegrationEvent(domain)).thenReturn(integrationEvent);

                publisher.onMerchantActivated(domain);

                verify(integrationEventPublisher).publish(integrationEvent);
        }
}
