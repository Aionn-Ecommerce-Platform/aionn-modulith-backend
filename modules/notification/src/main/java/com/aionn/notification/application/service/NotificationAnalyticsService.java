package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.analytics.result.AnalyticsResult;
import com.aionn.notification.application.port.out.NotificationPersistencePort;
import com.aionn.notification.domain.event.NotificationEvents;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationAnalyticsService {

    private final NotificationPersistencePort notificationRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public AnalyticsResult report(String campaignId) {
        long sent = notificationRepository.countByCampaignAndStatus(campaignId, "SENT");
        long read = notificationRepository.countByCampaignAndStatus(campaignId, "READ");
        long failed = notificationRepository.countByCampaignAndStatus(campaignId, "FAILED");
        Instant now = clock.instant();
        String reportId = "ana-" + IdGenerator.ulid();

        NotificationEvents.AnalyticsReportGenerated event = new NotificationEvents.AnalyticsReportGenerated(
                reportId, campaignId, (int) sent, (int) read, (int) failed, now, now);

        eventPublisher.publish(new EventEnvelope(
                IdGenerator.ulid(),
                "NotificationAnalytics",
                reportId,
                event,
                event.occurredAt()));

        return new AnalyticsResult(reportId, campaignId, (int) sent, (int) read, (int) failed, now);
    }
}
