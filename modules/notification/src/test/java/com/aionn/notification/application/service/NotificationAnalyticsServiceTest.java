package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.analytics.result.AnalyticsResult;
import com.aionn.notification.application.port.out.NotificationPersistencePort;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationAnalyticsServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private NotificationPersistencePort notificationRepository;
    @Mock
    private EventPublisher eventPublisher;

    private NotificationAnalyticsService service() {
        return new NotificationAnalyticsService(notificationRepository, eventPublisher, CLOCK);
    }

    @Test
    void reportAggregatesCountsPerStatus() {
        when(notificationRepository.countByCampaignAndStatus("camp-1", "SENT")).thenReturn(10L);
        when(notificationRepository.countByCampaignAndStatus("camp-1", "READ")).thenReturn(4L);
        when(notificationRepository.countByCampaignAndStatus("camp-1", "FAILED")).thenReturn(1L);

        AnalyticsResult result = service().report("camp-1");

        assertThat(result.campaignId()).isEqualTo("camp-1");
        assertThat(result.sentCount()).isEqualTo(10);
        assertThat(result.readCount()).isEqualTo(4);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.generatedAt()).isEqualTo(NOW);
        assertThat(result.reportId()).startsWith("ana-");
    }

    @Test
    void reportPublishesAnalyticsEnvelope() {
        when(notificationRepository.countByCampaignAndStatus("camp-1", "SENT")).thenReturn(0L);
        when(notificationRepository.countByCampaignAndStatus("camp-1", "READ")).thenReturn(0L);
        when(notificationRepository.countByCampaignAndStatus("camp-1", "FAILED")).thenReturn(0L);

        AnalyticsResult result = service().report("camp-1");

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().aggregateType()).isEqualTo("NotificationAnalytics");
        assertThat(captor.getValue().aggregateId()).isEqualTo(result.reportId());
        assertThat(captor.getValue().occurredAt()).isEqualTo(NOW);
    }
}
