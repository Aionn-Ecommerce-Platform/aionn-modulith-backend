package com.aionn.chat.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.chat.domain.event.ChatEvents;
import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import com.aionn.chat.domain.valueobject.MessagePayload;
import com.aionn.chat.domain.valueobject.MessageStatus;
import com.aionn.chat.domain.valueobject.MessageType;
import com.aionn.chat.domain.valueobject.ParticipantRole;
import lombok.Getter;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class Message extends AggregateRoot {

    public static final int MAX_TEXT_LENGTH = 4000;
    public static final Duration RECALL_WINDOW = Duration.ofMinutes(2);

    private final String messageId;
    private final String conversationId;
    private final String senderId;
    private final ParticipantRole senderRole;
    private final MessageType type;
    private MessagePayload payload;
    private MessageStatus status;
    private final Set<String> deliveredTo;
    private final Set<String> readBy;
    private boolean recalled;
    private final Instant sentAt;
    private Instant updatedAt;

    public Message(
            String messageId,
            String conversationId,
            String senderId,
            ParticipantRole senderRole,
            MessageType type,
            MessagePayload payload,
            MessageStatus status,
            Set<String> deliveredTo,
            Set<String> readBy,
            boolean recalled,
            Instant sentAt,
            Instant updatedAt) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.type = type;
        this.payload = payload;
        this.status = status;
        this.deliveredTo = deliveredTo == null ? new HashSet<>() : new HashSet<>(deliveredTo);
        this.readBy = readBy == null ? new HashSet<>() : new HashSet<>(readBy);
        this.recalled = recalled;
        this.sentAt = sentAt;
        this.updatedAt = updatedAt;
    }

    public static Message send(
            String messageId,
            String conversationId,
            String senderId,
            ParticipantRole senderRole,
            MessageType type,
            MessagePayload payload,
            List<String> recipientIds) {
        return send(messageId, conversationId, senderId, senderRole, type, payload, recipientIds,
                Clock.systemUTC());
    }

    public static Message send(
            String messageId,
            String conversationId,
            String senderId,
            ParticipantRole senderRole,
            MessageType type,
            MessagePayload payload,
            List<String> recipientIds,
            Clock clock) {
        validatePayload(type, payload);
        Instant now = clock.instant();
        Message m = new Message(messageId, conversationId, senderId, senderRole, type, payload,
                MessageStatus.SENT, new HashSet<>(), new HashSet<>(), false, now, now);
        m.registerEvent(new ChatEvents.MessageSent(conversationId, messageId, senderId, senderRole,
                List.copyOf(recipientIds), type, payload, now));
        return m;
    }

    public void markDeliveredTo(String userId) {
        markDeliveredTo(userId, Clock.systemUTC());
    }

    public void markDeliveredTo(String userId, Clock clock) {
        if (recalled || senderId.equals(userId))
            return;
        boolean changed = deliveredTo.add(userId);
        if (!changed)
            return;
        if (status == MessageStatus.SENT) {
            this.status = MessageStatus.DELIVERED;
        }
        Instant now = clock.instant();
        this.updatedAt = now;
        registerEvent(new ChatEvents.MessageDelivered(conversationId, messageId, userId, now));
    }

    public void markReadBy(String userId) {
        markReadBy(userId, Clock.systemUTC());
    }

    public void markReadBy(String userId, Clock clock) {
        if (recalled || senderId.equals(userId))
            return;
        boolean changed = readBy.add(userId);
        deliveredTo.add(userId);
        if (!changed)
            return;
        if (status != MessageStatus.RECALLED) {
            this.status = MessageStatus.READ;
        }
        Instant now = clock.instant();
        this.updatedAt = now;
        registerEvent(new ChatEvents.MessageRead(conversationId, messageId, userId, now));
    }

    public void recall(String userId) {
        recall(userId, Clock.systemUTC());
    }

    public void recall(String userId, Clock clock) {
        Guard.require(senderId.equals(userId),
                () -> new ChatException(ChatErrorCode.MESSAGE_FORBIDDEN));
        Guard.require(!recalled,
                () -> new ChatException(ChatErrorCode.MESSAGE_ALREADY_RECALLED));
        Instant now = clock.instant();
        Guard.require(!now.isAfter(sentAt.plus(RECALL_WINDOW)),
                () -> new ChatException(ChatErrorCode.MESSAGE_RECALL_WINDOW_EXPIRED));
        this.recalled = true;
        this.status = MessageStatus.RECALLED;
        this.payload = MessagePayload.system("(recalled)");
        this.updatedAt = now;
        registerEvent(new ChatEvents.MessageRecalled(conversationId, messageId, senderId, now));
    }

    public String previewBody() {
        if (recalled)
            return "(recalled)";
        return switch (type) {
            case TEXT, SYSTEM -> payload.body() == null ? "" : payload.body();
            case IMAGE -> "[Photo]";
            case PRODUCT_CARD -> "[Product]";
            case ORDER_REF -> "[Order]";
        };
    }

    private static void validatePayload(MessageType type, MessagePayload payload) {
        Guard.require(payload != null,
                () -> new ChatException(ChatErrorCode.MESSAGE_EMPTY));
        if (type == MessageType.TEXT) {
            String body = payload.body();
            Guard.require(body != null && !body.isBlank(),
                    () -> new ChatException(ChatErrorCode.MESSAGE_EMPTY));
            Guard.require(body.length() <= MAX_TEXT_LENGTH,
                    () -> new ChatException(ChatErrorCode.MESSAGE_TOO_LONG));
        }
        Guard.require(type != MessageType.IMAGE || payload.metadata().containsKey("imageUrl"),
                () -> new ChatException(ChatErrorCode.INVALID_ARGUMENT, "imageUrl required for IMAGE message"));
        Guard.require(type != MessageType.PRODUCT_CARD || payload.metadata().containsKey("productId"),
                () -> new ChatException(ChatErrorCode.INVALID_ARGUMENT, "productId required for PRODUCT_CARD message"));
        Guard.require(type != MessageType.ORDER_REF || payload.metadata().containsKey("orderId"),
                () -> new ChatException(ChatErrorCode.INVALID_ARGUMENT, "orderId required for ORDER_REF message"));
    }

    @Override
    protected String aggregateId() {
        return messageId;
    }
}
