package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.port.out.ChannelSender;
import com.aionn.notification.application.port.out.RecipientResolver;
import com.aionn.notification.application.port.out.observability.NotificationMetricsPort;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.Notification;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.notification.domain.valueobject.NotificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryOrchestrator {

    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_FAILED = "failed";

    private final NotificationDispatchService dispatchService;
    private final RecipientResolver recipientResolver;
    private final NotificationMetricsPort metrics;
    private final List<ChannelSender> channelSenders;

    private Map<NotificationChannel, ChannelSender> senderIndex;

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
        ChannelSender.DeliveryResult delivery;
        try {
            delivery = channelSender.send(new ChannelSender.DeliveryRequest(
                    notification.getNotiId(), notification.getUserId(), recipient,
                    notification.getSubject(), notification.getContent()));
        } catch (RuntimeException ex) {
            delivery = ChannelSender.DeliveryResult.failed("SEND_EXCEPTION", ex.getMessage());
        }

        if (delivery.success()) {
            metrics.deliveryOutcome(channel, OUTCOME_SUCCESS);
            return dispatchService.recordSent(new NotificationCommands.RecordSent(notification.getNotiId()));
        }
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
        if (senderIndex == null) {
            EnumMap<NotificationChannel, ChannelSender> index = new EnumMap<>(NotificationChannel.class);
            for (ChannelSender candidate : channelSenders) {
                index.put(candidate.channel(), candidate);
            }
            senderIndex = index;
        }
        ChannelSender sender = senderIndex.get(channel);
        if (sender == null) {
            throw new NotificationException(NotificationErrorCode.PROVIDER_NOT_FOUND,
                    "No sender wired for " + channel);
        }
        return sender;
    }
}
