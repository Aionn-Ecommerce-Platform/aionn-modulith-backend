package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.port.out.ChannelSender;
import com.aionn.notification.application.port.out.DeliveryAttemptPort;
import com.aionn.notification.application.port.out.RecipientResolver;
import com.aionn.notification.application.port.out.observability.NotificationMetricsPort;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.Notification;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.notification.domain.valueobject.NotificationStatus;
import lombok.extern.slf4j.Slf4j;
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
    private final Map<NotificationChannel, ChannelSender> senderIndex;

    public NotificationDeliveryOrchestrator(NotificationDispatchService dispatchService,
            RecipientResolver recipientResolver, NotificationMetricsPort metrics,
            DeliveryAttemptPort deliveryAttemptPort, List<ChannelSender> channelSenders) {
        this.dispatchService = dispatchService;
        this.recipientResolver = recipientResolver;
        this.metrics = metrics;
        this.deliveryAttemptPort = deliveryAttemptPort;
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
        String recipient;
        try {
            recipient = resolveRecipient(notification, recipientOverride);
        } catch (RuntimeException ex) {
            metrics.deliveryOutcome(channel, OUTCOME_FAILED);
            return dispatchService.recordFailed(new NotificationCommands.RecordFailed(
                    notification.getNotiId(), "RECIPIENT_RESOLVE_FAILED:" + ex.getMessage()));
        }

        ChannelSender channelSender = sender(notification.getChannel());
        DeliveryAttemptPort.Attempt attempt = deliveryAttemptPort.begin(
                notification.getNotiId(), notification.getChannel());
        if (attempt.status() == DeliveryAttemptPort.Status.SUCCEEDED) {
            return dispatchService.recordSent(new NotificationCommands.RecordSent(notification.getNotiId()));
        }
        if (!attempt.created()) {
            metrics.deliveryOutcome(channel, OUTCOME_FAILED);
            return dispatchService.recordDeliveryUnknown(notification.getNotiId(),
                    "A previous provider call did not persist a definitive outcome");
        }
        ChannelSender.DeliveryResult delivery;
        try {
            delivery = channelSender.send(new ChannelSender.DeliveryRequest(
                    notification.getNotiId(), notification.getUserId(), recipient,
                    notification.getSubject(), notification.getContent()));
        } catch (RuntimeException ex) {
            metrics.deliveryOutcome(channel, OUTCOME_FAILED);
            return dispatchService.recordDeliveryUnknown(notification.getNotiId(), ex.getMessage());
        }

        if (delivery.success()) {
            deliveryAttemptPort.recordSucceeded(attempt.attemptId(), delivery.externalId());
            metrics.deliveryOutcome(channel, OUTCOME_SUCCESS);
            return dispatchService.recordSent(new NotificationCommands.RecordSent(notification.getNotiId()));
        }
        deliveryAttemptPort.recordFailed(attempt.attemptId(),
                delivery.errorCode() + ":" + delivery.errorReason());
        metrics.deliveryOutcome(channel, OUTCOME_FAILED);
        return dispatchService.recordFailed(new NotificationCommands.RecordFailed(
                notification.getNotiId(), delivery.errorCode() + ":" + delivery.errorReason()));
    }

    private String resolveRecipient(Notification notification, String recipientOverride) {
        if (recipientOverride != null && !recipientOverride.isBlank()) {
            return recipientOverride;
        }
        return recipientResolver.resolve(notification.getUserId(), notification.getChannel());
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
