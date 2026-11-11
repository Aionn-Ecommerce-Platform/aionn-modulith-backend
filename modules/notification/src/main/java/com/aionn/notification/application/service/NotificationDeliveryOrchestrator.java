package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.port.out.ChannelSender;
import com.aionn.notification.application.port.out.DeliveryAttemptPort;
import com.aionn.notification.application.port.out.NotificationProviderPersistencePort;
import com.aionn.notification.application.port.out.RecipientResolver;
import com.aionn.notification.application.port.out.observability.NotificationMetricsPort;
import com.aionn.sharedkernel.application.port.RateLimiterPort;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.Notification;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.notification.domain.valueobject.NotificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NotificationDeliveryOrchestrator {

    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_FAILED = "failed";

    private final NotificationDispatchService dispatchService;
    private final RecipientResolver recipientResolver;
    private final NotificationMetricsPort metrics;
    private final DeliveryAttemptPort deliveryAttemptPort;
    private final NotificationProviderPersistencePort providerRepository;
    private final RateLimiterPort rateLimiter;
    private final Map<NotificationChannel, ChannelSender> senderIndex;

    public NotificationDeliveryOrchestrator(NotificationDispatchService dispatchService,
            RecipientResolver recipientResolver, NotificationMetricsPort metrics,
            DeliveryAttemptPort deliveryAttemptPort, NotificationProviderPersistencePort providerRepository,
            RateLimiterPort rateLimiter, List<ChannelSender> channelSenders) {
        this.dispatchService = dispatchService;
        this.recipientResolver = recipientResolver;
        this.metrics = metrics;
        this.deliveryAttemptPort = deliveryAttemptPort;
        this.providerRepository = providerRepository;
        this.rateLimiter = rateLimiter;
        EnumMap<NotificationChannel, ChannelSender> index = new EnumMap<>(NotificationChannel.class);
        for (ChannelSender candidate : channelSenders) {
            if (index.putIfAbsent(candidate.channel(), candidate) != null) {
                throw new IllegalStateException("Duplicate notification sender for " + candidate.channel());
            }
        }
        this.senderIndex = Map.copyOf(index);
    }

    public List<Notification> sendByEvent(NotificationCommands.SendByEvent command) {
        List<Notification> prepared = dispatchService.prepareByEvent(command);
        List<Notification> delivered = new ArrayList<>(prepared.size());
        for (Notification notification : prepared) {
            delivered.add(deliver(notification, null));
        }
        return delivered;
    }

    /**
     * Persists the notifications and returns without contacting any provider.
     *
     * <p>Event listeners run on the outbox dispatcher thread, which is single-threaded and shared by
     * every module. Sending SMTP, SMS or push there turns one slow provider call into a stall of all
     * order, payment and shipping events. Queued notifications are delivered by
     * {@code NotificationRetryScheduler}, which already owns retry, backoff and attempt tracking.
     */
    public List<Notification> enqueueByEvent(NotificationCommands.SendByEvent command) {
        return dispatchService.prepareByEvent(command);
    }

    /** Persists a single notification for later delivery by the retry worker. */
    public Notification enqueueDirectByEvent(NotificationCommands.SendDirectByEvent command) {
        return dispatchService.prepareDirect(command);
    }

    /**
     * Delivers notifications that were already persisted by {@link #enqueueByEvent}.
     *
     * <p>Runs on {@code notificationTaskExecutor} so SMTP, SMS or push latency never blocks the outbox
     * dispatcher thread. The rows are already committed as {@code PENDING}, so a rejected or lost task
     * is picked up by the retry sweep instead of disappearing.
     */
    @Async("notificationTaskExecutor")
    public void deliverQueuedAsync(List<Notification> queued) {
        if (queued == null || queued.isEmpty()) {
            return;
        }
        for (Notification notification : queued) {
            try {
                deliver(notification, null);
            } catch (RuntimeException ex) {
                log.warn("Queued delivery failed for {} on {}: {}",
                        notification.getNotiId(), notification.getChannel(), ex.getMessage());
            }
        }
    }

    public Notification sendDirectByEvent(NotificationCommands.SendDirectByEvent command) {
        return deliver(dispatchService.prepareDirect(command), command.recipient());
    }

    public int retryPending(int batchSize) {
        int succeeded = 0;
        for (Notification pending : dispatchService.findRetryable(batchSize)) {
            try {
                if (deliver(pending, null).getStatus() == NotificationStatus.SENT) {
                    succeeded++;
                }
            } catch (RuntimeException ex) {
                log.warn("Retry failed for {}: {}", pending.getNotiId(), ex.getMessage());
            }
        }
        return succeeded;
    }

    private Notification deliver(Notification notification, String recipientOverride) {
        String channel = notification.getChannel().name();
        List<String> recipients;
        try {
            recipients = resolveRecipients(notification, recipientOverride);
        } catch (RuntimeException ex) {
            metrics.deliveryOutcome(channel, OUTCOME_FAILED);
            return dispatchService.recordFailed(new NotificationCommands.RecordFailed(
                    notification.getNotiId(), "RECIPIENT_RESOLVE_FAILED:" + ex.getMessage()));
        }

        if (rateLimited(notification)) {
            metrics.deliveryOutcome(channel, "rate_limited");
            log.info("Notification {} on channel {} is rate limited; leaving it pending",
                    notification.getNotiId(), channel);
            return notification;
        }

        ChannelSender channelSender = sender(notification.getChannel());
        DeliveryAttemptPort.Attempt attempt = deliveryAttemptPort.begin(
                notification.getNotiId(), notification.getChannel());
        if (attempt.status() == DeliveryAttemptPort.Status.SUCCEEDED) {
            return dispatchService.recordSent(new NotificationCommands.RecordSent(notification.getNotiId()));
        }
        if (!attempt.created()) {
            // Another worker is currently delivering this notification. Do not
            // mark it FAILED: that would strand a message whose provider call may
            // still succeed. The next retry sweep will observe the real outcome.
            log.info("Delivery already in progress for notification {}", notification.getNotiId());
            return notification;
        }

        int delivered = 0;
        String lastError = null;
        String externalId = null;
        boolean ambiguous = false;
        for (String recipient : recipients) {
            try {
                ChannelSender.DeliveryResult result = channelSender.send(new ChannelSender.DeliveryRequest(
                        notification.getNotiId(), notification.getUserId(), recipient,
                        notification.getSubject(), notification.getContent()));
                if (result.success()) {
                    delivered++;
                    externalId = result.externalId();
                } else {
                    lastError = result.errorCode() + ":" + result.errorReason();
                }
            } catch (RuntimeException ex) {
                ambiguous = true;
                lastError = ex.getMessage();
            }
        }

        if (delivered > 0) {
            deliveryAttemptPort.recordSucceeded(attempt.attemptId(), externalId);
            metrics.deliveryOutcome(channel, OUTCOME_SUCCESS);
            return dispatchService.recordSent(new NotificationCommands.RecordSent(notification.getNotiId()));
        }
        if (ambiguous) {
            metrics.deliveryOutcome(channel, OUTCOME_FAILED);
            return dispatchService.recordDeliveryUnknown(notification.getNotiId(), lastError);
        }
        deliveryAttemptPort.recordFailed(attempt.attemptId(),
                lastError == null ? "DELIVERY_FAILED" : lastError);
        metrics.deliveryOutcome(channel, OUTCOME_FAILED);
        return dispatchService.recordFailed(new NotificationCommands.RecordFailed(
                notification.getNotiId(), lastError == null ? "DELIVERY_FAILED" : lastError));
    }

    private boolean rateLimited(Notification notification) {
        return providerRepository.findActiveByChannel(notification.getChannel())
                .map(provider -> provider.getRateLimitPerMinute() > 0
                        && !rateLimiter.allow("notification:rate-limit",
                                notification.getChannel().name(),
                                provider.getRateLimitPerMinute(), 60))
                .orElse(false);
    }

    private List<String> resolveRecipients(Notification notification, String recipientOverride) {
        if (recipientOverride != null && !recipientOverride.isBlank()) {
            return List.of(recipientOverride);
        }
        List<String> recipients = recipientResolver.resolveAll(notification.getUserId(), notification.getChannel());
        if (recipients == null || recipients.isEmpty()) {
            return List.of(recipientResolver.resolve(notification.getUserId(), notification.getChannel()));
        }
        return recipients;
    }

    private ChannelSender sender(NotificationChannel channel) {
        ChannelSender sender = senderIndex.get(channel);
        if (sender == null) {
            throw new NotificationException(NotificationErrorCode.PROVIDER_NOT_FOUND,
                    "No sender wired for " + channel);
        }
        return sender;
    }
}
