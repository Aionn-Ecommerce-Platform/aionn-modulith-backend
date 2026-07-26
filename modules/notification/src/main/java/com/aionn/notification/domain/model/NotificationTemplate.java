package com.aionn.notification.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.notification.domain.event.NotificationEvents;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class NotificationTemplate extends AggregateRoot {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.]+)\\s*\\}\\}");

    private final String templateId;
    private final String eventType;
    private final NotificationChannel channel;
    private final NotificationCategory category;
    private final String locale;
    private String subject;
    private String content;
    private final List<String> placeholders;
    private int version;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public NotificationTemplate(
            String templateId,
            String eventType,
            NotificationChannel channel,
            NotificationCategory category,
            String locale,
            String subject,
            String content,
            List<String> placeholders,
            int version,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        this.templateId = templateId;
        this.eventType = eventType;
        this.channel = channel;
        this.category = category;
        this.locale = locale;
        this.subject = subject;
        this.content = content;
        this.placeholders = placeholders == null ? new ArrayList<>() : new ArrayList<>(placeholders);
        this.version = version;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NotificationTemplate create(
            String templateId,
            String eventType,
            NotificationChannel channel,
            NotificationCategory category,
            String locale,
            String subject,
            String content) {
        return create(templateId, eventType, channel, category, locale, subject, content,
                Clock.systemUTC());
    }

    public static NotificationTemplate create(
            String templateId,
            String eventType,
            NotificationChannel channel,
            NotificationCategory category,
            String locale,
            String subject,
            String content,
            Clock clock) {
        if (eventType == null || eventType.isBlank()) {
            throw new NotificationException(NotificationErrorCode.INVALID_ARGUMENT, "eventType required");
        }
        if (content == null || content.isBlank()) {
            throw new NotificationException(NotificationErrorCode.INVALID_ARGUMENT, "content required");
        }
        List<String> extracted = extractPlaceholders(content);
        Instant now = clock.instant();
        NotificationTemplate t = new NotificationTemplate(templateId, eventType, channel, category,
                locale == null ? "vi-VN" : locale, subject, content, extracted, 1, true, now, now);
        t.registerEvent(new NotificationEvents.TemplateCreated(templateId, eventType, content,
                List.copyOf(extracted), now));
        return t;
    }

    public void update(String subject, String content) {
        update(subject, content, Clock.systemUTC());
    }

    public void update(String subject, String content, Clock clock) {
        if (content == null || content.isBlank()) {
            throw new NotificationException(NotificationErrorCode.INVALID_ARGUMENT, "content required");
        }
        this.subject = subject;
        this.content = content;
        this.placeholders.clear();
        this.placeholders.addAll(extractPlaceholders(content));
        this.version++;
        Instant now = clock.instant();
        this.updatedAt = now;
        registerEvent(new NotificationEvents.TemplateUpdated(templateId, content, version, now));
    }

    public Rendered render(Map<String, String> context) {
        for (String key : placeholders) {
            Guard.require(context.containsKey(key),
                    () -> new NotificationException(NotificationErrorCode.TEMPLATE_PLACEHOLDER_MISSING,
                            "Missing placeholder: " + key));
        }
        String renderedContent = applyPlaceholders(content, context);
        String renderedSubject = subject == null ? null : applyPlaceholders(subject, context);
        return new Rendered(renderedSubject, renderedContent);
    }

    private static String applyPlaceholders(String input, Map<String, String> context) {
        Matcher m = PLACEHOLDER.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String value = context.getOrDefault(key, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static List<String> extractPlaceholders(String content) {
        List<String> keys = new ArrayList<>();
        Matcher m = PLACEHOLDER.matcher(content);
        while (m.find()) {
            String key = m.group(1);
            if (!keys.contains(key))
                keys.add(key);
        }
        return keys;
    }

    public record Rendered(String subject, String content) {
    }

    @Override
    protected String aggregateId() {
        return templateId;
    }
}
