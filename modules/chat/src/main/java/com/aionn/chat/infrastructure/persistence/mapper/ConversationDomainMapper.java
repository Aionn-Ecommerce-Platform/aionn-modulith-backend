package com.aionn.chat.infrastructure.persistence.mapper;

import com.aionn.chat.domain.model.Conversation;
import com.aionn.chat.domain.valueobject.MessageType;
import com.aionn.chat.domain.valueobject.Participant;
import com.aionn.chat.domain.valueobject.ParticipantRole;
import com.aionn.chat.infrastructure.persistence.entity.ConversationEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConversationDomainMapper {

    public Conversation toDomain(ConversationEntity e) {
        List<Participant> participants = new ArrayList<>();
        if (e.getParticipants() != null) {
            for (Map<String, Object> raw : e.getParticipants()) {
                participants.add(new Participant(
                        (String) raw.get("userId"),
                        ParticipantRole.valueOf((String) raw.get("role")),
                        (String) raw.get("displayName"),
                        (String) raw.get("avatarUrl"),
                        parseInstant(raw.get("joinedAt")),
                        parseInstant(raw.get("lastReadAt"))));
            }
        }
        return new Conversation(
                e.getConversationId(),
                e.getBuyerId(),
                e.getMerchantId(),
                participants,
                e.getLastMessageId(),
                e.getLastMessagePreview(),
                e.getLastMessageType() == null ? null : MessageType.valueOf(e.getLastMessageType()),
                e.getLastMessageSenderId(),
                e.getLastMessageAt(),
                e.isArchived(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public ConversationEntity toEntity(Conversation c, ConversationEntity existing) {
        ConversationEntity entity = existing != null ? existing
                : ConversationEntity.builder()
                        .conversationId(c.getConversationId())
                        .buyerId(c.getBuyerId())
                        .merchantId(c.getMerchantId())
                        .build();
        List<Map<String, Object>> raw = new ArrayList<>();
        for (Participant p : c.participants()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", p.userId());
            m.put("role", p.role().name());
            m.put("displayName", p.displayName());
            m.put("avatarUrl", p.avatarUrl());
            m.put("joinedAt", p.joinedAt() == null ? null : p.joinedAt().toString());
            m.put("lastReadAt", p.lastReadAt() == null ? null : p.lastReadAt().toString());
            raw.add(m);
        }
        entity.setParticipants(raw);
        entity.setLastMessageId(c.getLastMessageId());
        entity.setLastMessagePreview(c.getLastMessagePreview());
        entity.setLastMessageType(c.getLastMessageType() == null ? null : c.getLastMessageType().name());
        entity.setLastMessageSenderId(c.getLastMessageSenderId());
        entity.setLastMessageAt(c.getLastMessageAt());
        entity.setArchived(c.isArchived());
        return entity;
    }

    private static Instant parseInstant(Object value) {
        if (value == null)
            return null;
        if (value instanceof Instant i)
            return i;
        return Instant.parse(value.toString());
    }
}

