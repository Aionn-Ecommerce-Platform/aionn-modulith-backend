package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.port.out.NotificationPersistencePort;
import com.aionn.notification.application.port.out.NotificationSubscriptionPersistencePort;
import com.aionn.notification.application.port.out.NotificationTemplatePersistencePort;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.Notification;
import com.aionn.notification.domain.model.NotificationSubscription;
import com.aionn.notification.domain.model.NotificationTemplate;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationDispatchService {

    private static final String DEFAULT_LOCALE = "vi-VN";
    private static final String ENGLISH_LOCALE = "en-US";

    private final NotificationPersistencePort notificationRepository;
    private final NotificationTemplatePersistencePort templateRepository;
    private final NotificationSubscriptionPersistencePort subscriptionRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public List<Notification> prepareByEvent(NotificationCommands.SendByEvent command) {
        NotificationSubscription subscription = subscriptionRepository.findByUserId(command.userId())
                .orElseGet(() -> subscriptionRepository.save(
                        NotificationSubscription.createDefault(command.userId(), clock)));

        List<NotificationChannel> requested = command.channels() == null || command.channels().isEmpty()
                ? Arrays.asList(NotificationChannel.values())
                : command.channels();

        String locale = resolveLocale(command.locale());
        List<Notification> prepared = new ArrayList<>();
        for (NotificationChannel channel : requested) {
            if (!subscription.isEnabled(command.category(), channel)) {
                log.debug("Skipping {} for user {} - subscription disabled", channel, command.userId());
                continue;
            }
            NotificationTemplate template = findTemplate(command.eventType(), channel, locale).orElse(null);
            if (template == null) {
                log.warn("No template for event={} channel={} locale={}; skipping",
                        command.eventType(), channel, locale);
                continue;
            }
            prepared.add(persistPending(template, command.userId(), channel,
                    command.category(), command.campaignId(), command.context()));
        }
        return prepared;
    }

    public Notification prepareDirect(NotificationCommands.SendDirectByEvent command) {
        String locale = resolveLocale(command.locale());
        NotificationTemplate template = findTemplate(command.eventType(), command.channel(), locale)
                .orElseThrow(() -> new NotificationException(
                        NotificationErrorCode.TEMPLATE_NOT_FOUND,
                        "No template for event=" + command.eventType()
                                + " channel=" + command.channel()
                                + " locale=" + locale));
        return persistPending(template, command.userId(), command.channel(),
                command.category(), command.campaignId(), command.context());
    }

    public Notification recordSent(NotificationCommands.RecordSent command) {
        Notification notification = required(command.notiId());
        notification.markSent(clock);
        Notification saved = notificationRepository.save(notification);
        eventPublisher.publish(notification.pullEvents());
        return saved;
    }

    public Notification recordFailed(NotificationCommands.RecordFailed command) {
        Notification notification = required(command.notiId());
        notification.markFailed(command.reason(), clock);
        Notification saved = notificationRepository.save(notification);
        eventPublisher.publish(notification.pullEvents());
        return saved;
    }

    public Notification markRead(NotificationCommands.MarkRead command) {
        Notification notification = ownedBy(command.notiId(), command.userId());
        notification.markRead(clock);
        Notification saved = notificationRepository.save(notification);
        eventPublisher.publish(notification.pullEvents());
        return saved;
    }

    public Notification delete(NotificationCommands.MarkDeleted command) {
        Notification notification = ownedBy(command.notiId(), command.userId());
        notification.softDelete(clock);
        Notification saved = notificationRepository.save(notification);
        eventPublisher.publish(notification.pullEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public Notification get(String userId, String notiId) {
        return ownedBy(notiId, userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> listMine(String userId, int limit) {
        return notificationRepository.findByUser(userId, limit);
    }

    @Transactional(readOnly = true)
    public List<Notification> findRetryable(int batchSize) {
        return notificationRepository.findRetryable(batchSize);
    }

    private Notification persistPending(NotificationTemplate template, String userId,
            NotificationChannel channel, com.aionn.notification.domain.valueobject.NotificationCategory category,
            String campaignId, Map<String, String> context) {
        NotificationTemplate.Rendered rendered = template.render(context == null ? Map.of() : context);
        Notification notification = Notification.create(IdGenerator.ulid(), userId,
                template.getTemplateId(), channel, category,
                rendered.subject(), rendered.content(), campaignId, clock);
        return notificationRepository.save(notification);
    }

    private java.util.Optional<NotificationTemplate> findTemplate(String eventType,
            NotificationChannel channel, String locale) {
        return templateRepository.findByEventChannelLocale(eventType, channel, locale)
                .or(() -> templateRepository.findByEventChannelLocale(eventType, channel, DEFAULT_LOCALE));
    }

    private static String resolveLocale(String requested) {
        if (requested != null) {
            return requested;
        }
        Locale current = LocaleContextHolder.getLocale();
        return "en".equalsIgnoreCase(current.getLanguage()) ? ENGLISH_LOCALE : DEFAULT_LOCALE;
    }

    private Notification ownedBy(String notiId, String userId) {
        Notification notification = required(notiId);
        notification.ensureOwnedBy(userId);
        return notification;
    }

    private Notification required(String notiId) {
        return notificationRepository.findById(notiId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }
}
