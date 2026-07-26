package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.analytics.result.AnalyticsResult;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.notification.application.port.out.NotificationPersistencePort;
import com.aionn.notification.domain.event.NotificationEvents;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationAnalyticsService {

    private final NotificationPersistencePort notificationRepository;
    private final EventPublisher eventPublisher;

    public AnalyticsResult report(String campaignId) {
        long sent = notificationRepository.countByCampaignAndStatus(campaignId, "SENT");
        long read = notificationRepository.countByCampaignAndStatus(campaignId, "READ");
        long failed = notificationRepository.countByCampaignAndStatus(campaignId, "FAILED");
        Instant now = Instant.now();
        String reportId = "ana-" + IdGenerator.ulid();

        NotificationEvents.AnalyticsReportGenerated event = new NotificationEvents.AnalyticsReportGenerated(
                reportId, campaignId, (int) sent, (int) read, (int) failed, now, now);

        EventEnvelope envelope = new EventEnvelope(
                IdGenerator.ulid(),
                "NotificationAnalytics",
                reportId,
                event,
                event.occurredAt());

        eventPublisher.publish(envelope);

        return new AnalyticsResult(reportId, campaignId, (int) sent, (int) read, (int) failed, now);
    }
}
