package com.aionn.chat.infrastructure.persistence.mapper;

import com.aionn.chat.domain.model.Message;
import com.aionn.chat.domain.valueobject.MessagePayload;
import com.aionn.chat.domain.valueobject.MessageStatus;
import com.aionn.chat.domain.valueobject.MessageType;
import com.aionn.chat.domain.valueobject.ParticipantRole;
import com.aionn.chat.infrastructure.persistence.entity.MessageEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class MessageDomainMapper {

    public Message toDomain(MessageEntity e) {
        MessagePayload payload = new MessagePayload(e.getBody(), e.getMetadata());
        return new Message(
                e.getMessageId(),
                e.getConversationId(),
                e.getSenderId(),
                ParticipantRole.valueOf(e.getSenderRole()),
                MessageType.valueOf(e.getType()),
                payload,
                MessageStatus.valueOf(e.getStatus()),
                e.getDeliveredTo() == null ? new HashSet<>() : new HashSet<>(e.getDeliveredTo()),
                e.getReadBy() == null ? new HashSet<>() : new HashSet<>(e.getReadBy()),
                e.isRecalled(),
                e.getSentAt(),
                e.getUpdatedAt());
    }

    public MessageEntity toEntity(Message m, MessageEntity existing) {
        MessageEntity entity = existing != null ? existing
                : MessageEntity.builder()
                        .messageId(m.getMessageId())
                        .conversationId(m.getConversationId())
                        .senderId(m.getSenderId())
                        .senderRole(m.getSenderRole().name())
                        .type(m.getType().name())
                        .build();
        entity.setBody(m.getPayload().body());
        entity.setMetadata(m.getPayload().metadata());
        entity.setStatus(m.getStatus().name());
        entity.setDeliveredTo(new HashSet<>(m.getDeliveredTo()));
        entity.setReadBy(new HashSet<>(m.getReadBy()));
        entity.setRecalled(m.isRecalled());
        return entity;
    }
}

