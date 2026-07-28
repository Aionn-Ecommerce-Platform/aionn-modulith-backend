package com.aionn.chat.domain.model;

import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.chat.domain.event.ChatEvents;
import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import com.aionn.chat.domain.valueobject.MessageType;
import com.aionn.chat.domain.valueobject.Participant;
import com.aionn.chat.domain.valueobject.ParticipantRole;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
public class Conversation extends AggregateRoot {

    private final String conversationId;
    private final String buyerId;
    private final String merchantId;
    private final List<Participant> participants;
    private String lastMessageId;
    private String lastMessagePreview;
    private MessageType lastMessageType;
    private String lastMessageSenderId;
    private Instant lastMessageAt;
    private boolean archived;
    private final Instant createdAt;
    private Instant updatedAt;

    public Conversation(
            String conversationId,
            String buyerId,
            String merchantId,
            List<Participant> participants,
            String lastMessageId,
            String lastMessagePreview,
            MessageType lastMessageType,
            String lastMessageSenderId,
            Instant lastMessageAt,
            boolean archived,
            Instant createdAt,
            Instant updatedAt) {
        this.conversationId = conversationId;
        this.buyerId = buyerId;
        this.merchantId = merchantId;
        this.participants = new ArrayList<>(participants == null ? List.of() : participants);
        this.lastMessageId = lastMessageId;
        this.lastMessagePreview = lastMessagePreview;
        this.lastMessageType = lastMessageType;
        this.lastMessageSenderId = lastMessageSenderId;
        this.lastMessageAt = lastMessageAt;
        this.archived = archived;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Conversation start(
            String conversationId,
            String buyerId,
            String buyerDisplayName,
            String buyerAvatarUrl,
            String merchantId,
            String merchantDisplayName,
            String merchantAvatarUrl,
            String startedBy) {
        return start(conversationId, buyerId, buyerDisplayName, buyerAvatarUrl, merchantId,
                merchantDisplayName, merchantAvatarUrl, startedBy, Clock.systemUTC());
    }

    public static Conversation start(
            String conversationId,
            String buyerId,
            String buyerDisplayName,
            String buyerAvatarUrl,
            String merchantId,
            String merchantDisplayName,
            String merchantAvatarUrl,
            String startedBy,
            Clock clock) {
        if (buyerId.equals(merchantId)) {
            throw new ChatException(ChatErrorCode.INVALID_ARGUMENT, "buyerId must differ from merchantId");
        }
        Instant now = clock.instant();
        Participant buyer = new Participant(buyerId, ParticipantRole.BUYER,
                buyerDisplayName, buyerAvatarUrl, now, null);
        Participant merchant = new Participant(merchantId, ParticipantRole.MERCHANT,
                merchantDisplayName, merchantAvatarUrl, now, null);
        Conversation c = new Conversation(conversationId, buyerId, merchantId,
                List.of(buyer, merchant), null, null, null, null, null, false, now, now);
        c.registerEvent(new ChatEvents.ConversationStarted(conversationId,
                List.of(buyerId, merchantId), startedBy, now));
        return c;
    }

    public Participant requireParticipant(String userId) {
        return participants.stream()
                .filter(p -> p.userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ChatException(ChatErrorCode.CONVERSATION_FORBIDDEN));
    }

    public List<String> recipientsExcept(String senderId) {
        return participants.stream()
                .map(Participant::userId)
                .filter(id -> !id.equals(senderId))
                .toList();
    }

    public void joinSupport(String supportUserId, String displayName, String avatarUrl) {
        joinSupport(supportUserId, displayName, avatarUrl, Clock.systemUTC());
    }

    public void joinSupport(String supportUserId, String displayName, String avatarUrl, Clock clock) {
        Optional<Participant> existing = participants.stream()
                .filter(p -> p.userId().equals(supportUserId))
                .findFirst();
        if (existing.isPresent())
            return;
        Instant now = clock.instant();
        participants.add(new Participant(supportUserId, ParticipantRole.SUPPORT,
                displayName, avatarUrl, now, null));
        this.updatedAt = now;
    }

    public void recordMessageSent(String messageId, MessageType type, String preview, String senderId) {
        recordMessageSent(messageId, type, preview, senderId, Clock.systemUTC());
    }

    public void recordMessageSent(String messageId, MessageType type, String preview, String senderId,
            Clock clock) {
        requireParticipant(senderId);
        if (archived) {
            this.archived = false;
        }
        this.lastMessageId = messageId;
        this.lastMessageType = type;
        this.lastMessagePreview = preview;
        this.lastMessageSenderId = senderId;
        Instant now = clock.instant();
        this.lastMessageAt = now;
        this.updatedAt = now;
    }

    public void markRead(String userId) {
        markRead(userId, Clock.systemUTC());
    }

    public void markRead(String userId, Clock clock) {
        Participant participant = requireParticipant(userId);
        Instant now = clock.instant();
        replaceParticipant(participant.withLastReadAt(now));
        this.updatedAt = now;
        registerEvent(new ChatEvents.ConversationRead(conversationId, userId, now, now));
    }

    public int unreadCountFor(String userId, long totalMessages, long messagesByOther) {
        Participant participant = requireParticipant(userId);
        Instant lastRead = participant.lastReadAt();
        if (lastMessageAt == null)
            return 0;
        if (lastRead != null && !lastRead.isBefore(lastMessageAt))
            return 0;
        return (int) Math.max(0, messagesByOther);
    }

    public void archive(String userId) {
        archive(userId, Clock.systemUTC());
    }

    public void archive(String userId, Clock clock) {
        requireParticipant(userId);
        this.archived = true;
        this.updatedAt = clock.instant();
    }

    public void unarchive(String userId) {
        unarchive(userId, Clock.systemUTC());
    }

    public void unarchive(String userId, Clock clock) {
        requireParticipant(userId);
        this.archived = false;
        this.updatedAt = clock.instant();
    }

    public Map<String, Instant> participantLastReadMap() {
        Map<String, Instant> map = new LinkedHashMap<>();
        for (Participant p : participants) {
            map.put(p.userId(), p.lastReadAt());
        }
        return map;
    }

    public List<Participant> participants() {
        return List.copyOf(participants);
    }

    private void replaceParticipant(Participant updated) {
        for (int i = 0; i < participants.size(); i++) {
            if (participants.get(i).userId().equals(updated.userId())) {
                participants.set(i, updated);
                return;
            }
        }
    }

    @Override
    protected String aggregateId() {
        return conversationId;
    }
}
