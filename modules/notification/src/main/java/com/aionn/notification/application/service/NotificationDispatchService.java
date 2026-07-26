package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.dto.notification.result.NotificationResult;
import com.aionn.notification.application.mapper.NotificationResultMapper;
import com.aionn.notification.application.port.out.ChannelSender;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.notification.application.port.out.NotificationPersistencePort;
import com.aionn.notification.application.port.out.NotificationSubscriptionPersistencePort;
import com.aionn.notification.application.port.out.NotificationTemplatePersistencePort;
import com.aionn.notification.application.port.out.RecipientResolver;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.Notification;
import com.aionn.notification.domain.model.NotificationSubscription;
import com.aionn.notification.domain.model.NotificationTemplate;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationDispatchService {

    private final NotificationPersistencePort notificationRepository;
    private final NotificationTemplatePersistencePort templateRepository;
    private final NotificationSubscriptionPersistencePort subscriptionRepository;
    private final RecipientResolver recipientResolver;
    private final NotificationResultMapper mapper;
    private final EventPublisher eventPublisher;
    private final List<ChannelSender> channelSenders;

    private Map<NotificationChannel, ChannelSender> senderIndex;

    public List<NotificationResult> sendByEvent(NotificationCommands.SendByEvent command) {
        NotificationSubscription subscription = subscriptionRepository.findByUserId(command.userId())
                .orElseGet(() -> subscriptionRepository.save(NotificationSubscription.createDefault(command.userId())));

        List<NotificationChannel> requested = command.channels() == null || command.channels().isEmpty()
                ? Arrays.asList(NotificationChannel.values())
                : command.channels();

        List<NotificationResult> results = new ArrayList<>();
        for (NotificationChannel channel : requested) {
            if (!subscription.isEnabled(command.category(), channel)) {
                log.debug("Skipping {} for user {} - subscription disabled", channel, command.userId());
                continue;
            }
            final String locale;
            if (command.locale() != null) {
                locale = command.locale();
            } else {
                java.util.Locale current = org.springframework.context.i18n.LocaleContextHolder.getLocale();
                locale = "en".equalsIgnoreCase(current.getLanguage()) ? "en-US" : "vi-VN";
            }
            NotificationTemplate template = templateRepository.findByEventChannelLocale(
                    command.eventType(), channel, locale)
                    .or(() -> templateRepository.findByEventChannelLocale(command.eventType(), channel, "vi-VN"))
                    .orElse(null);
            if (template == null) {
                log.warn("No template for event={} channel={} locale={}; skipping",
                        command.eventType(), channel, locale);
                continue;
            }
            NotificationTemplate.Rendered rendered = template.render(
                    command.context() == null ? Map.of() : command.context());
            Notification notification = Notification.create(IdGenerator.ulid(),
                    command.userId(), template.getTemplateId(), channel, command.category(),
                    rendered.subject(), rendered.content(), command.campaignId());
            Notification saved = notificationRepository.save(notification);
            // Attempt delivery
            results.add(attemptDelivery(saved));
        }
        return results;
    }

    public NotificationResult sendDirectByEvent(NotificationCommands.SendDirectByEvent command) {
        final String locale;
        if (command.locale() != null) {
            locale = command.locale();
        } else {
            java.util.Locale current = org.springframework.context.i18n.LocaleContextHolder.getLocale();
            locale = "en".equalsIgnoreCase(current.getLanguage()) ? "en-US" : "vi-VN";
        }
        NotificationTemplate template = templateRepository.findByEventChannelLocale(
                command.eventType(), command.channel(), locale)
                .or(() -> templateRepository.findByEventChannelLocale(command.eventType(), command.channel(), "vi-VN"))
                .orElseThrow(() -> new NotificationException(
                        NotificationErrorCode.TEMPLATE_NOT_FOUND,
                        "No template for event=" + command.eventType()
                                + " channel=" + command.channel()
                                + " locale=" + locale));

        NotificationTemplate.Rendered rendered = template.render(
                command.context() == null ? Map.of() : command.context());
        Notification notification = Notification.create(IdGenerator.ulid(),
                command.userId(), template.getTemplateId(), command.channel(), command.category(),
                rendered.subject(), rendered.content(), command.campaignId());
        Notification saved = notificationRepository.save(notification);
        return attemptDelivery(saved, command.recipient());
    }

    public NotificationResult markRead(NotificationCommands.MarkRead command) {
        Notification n = ownedBy(command.notiId(), command.userId());
        n.markRead();
        Notification saved = notificationRepository.save(n);
        eventPublisher.publish(n.pullEvents());
        return mapper.toResult(saved);
    }

    public NotificationResult delete(NotificationCommands.MarkDeleted command) {
        Notification n = ownedBy(command.notiId(), command.userId());
        n.softDelete();
        Notification saved = notificationRepository.save(n);
        eventPublisher.publish(n.pullEvents());
        return mapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public NotificationResult get(String userId, String notiId) {
        return mapper.toResult(ownedBy(notiId, userId));
    }

    @Transactional(readOnly = true)
    public List<NotificationResult> listMine(String userId, int limit) {
        return notificationRepository.findByUser(userId, limit).stream()
                .map(mapper::toResult)
                .toList();
    }

public int retryPending(int batchSize) {
        List<Notification> retryable = notificationRepository.findRetryable(batchSize);
        int succeeded = 0;
        for (Notification n : retryable) {
            try {
                NotificationResult result = attemptDelivery(n);
                if ("SENT".equals(result.status()))
                    succeeded++;
            } catch (Exception ex) {
                log.warn("Retry failed for {}: {}", n.getNotiId(), ex.getMessage());
            }
        }
        return succeeded;
    }

    private NotificationResult attemptDelivery(Notification notification) {
        return attemptDelivery(notification, null);
    }

    private NotificationResult attemptDelivery(Notification notification, String recipientOverride) {
        ChannelSender sender = sender(notification.getChannel());
        String to;
        try {
            to = recipientOverride != null && !recipientOverride.isBlank()
                    ? recipientOverride
                    : recipientResolver.resolve(notification.getUserId(), notification.getChannel());
        } catch (RuntimeException ex) {
            notification.markFailed("RECIPIENT_RESOLVE_FAILED:" + ex.getMessage());
            Notification saved = notificationRepository.save(notification);
            eventPublisher.publish(notification.pullEvents());
            return mapper.toResult(saved);
        }
        ChannelSender.DeliveryResult delivery;
        try {
            delivery = sender.send(new ChannelSender.DeliveryRequest(
                    notification.getNotiId(), notification.getUserId(), to,
                    notification.getSubject(), notification.getContent()));
        } catch (RuntimeException ex) {
            delivery = ChannelSender.DeliveryResult.failed("SEND_EXCEPTION", ex.getMessage());
        }
        if (delivery.success()) {
            notification.markSent();
        } else {
            notification.markFailed(delivery.errorCode() + ":" + delivery.errorReason());
        }
        Notification saved = notificationRepository.save(notification);
        eventPublisher.publish(notification.pullEvents());
        return mapper.toResult(saved);
    }

    private Notification ownedBy(String notiId, String userId) {
        Notification n = notificationRepository.findById(notiId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        n.ensureOwnedBy(userId);
        return n;
    }

    private ChannelSender sender(NotificationChannel channel) {
        if (senderIndex == null) {
            EnumMap<NotificationChannel, ChannelSender> map = new EnumMap<>(NotificationChannel.class);
            for (ChannelSender s : channelSenders) {
                map.put(s.channel(), s);
            }
            senderIndex = map;
        }
        ChannelSender sender = senderIndex.get(channel);
        if (sender == null) {
            throw new NotificationException(NotificationErrorCode.PROVIDER_NOT_FOUND,
                    "No sender wired for " + channel);
        }
        return sender;
    }
}

