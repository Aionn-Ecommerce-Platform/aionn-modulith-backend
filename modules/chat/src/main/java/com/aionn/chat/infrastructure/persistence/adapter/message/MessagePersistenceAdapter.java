package com.aionn.chat.infrastructure.persistence.adapter.message;

import com.aionn.chat.application.port.out.MessagePersistencePort;
import com.aionn.chat.domain.model.Message;
import com.aionn.chat.infrastructure.persistence.entity.MessageEntity;
import com.aionn.chat.infrastructure.persistence.mapper.MessageDomainMapper;
import com.aionn.chat.infrastructure.persistence.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MessagePersistenceAdapter implements MessagePersistencePort {

    private final MessageRepository jpa;
    private final MessageDomainMapper mapper;

    @Override
    public Message save(Message message) {
        MessageEntity existing = jpa.findById(message.getMessageId()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(message, existing)));
    }

    @Override
    public Optional<Message> findById(String messageId) {
        return jpa.findById(messageId).map(mapper::toDomain);
    }

    @Override
    public List<Message> findByConversationBefore(String conversationId, Instant before, int limit) {
        return jpa.findByConversationBefore(conversationId, before, PageRequest.of(0, Math.max(1, limit)))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Message> findByConversationLatest(String conversationId, int limit) {
        return jpa.findByConversationLatest(conversationId, PageRequest.of(0, Math.max(1, limit)))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countUnread(String conversationId, String userId, Instant afterInstant) {
        return jpa.countUnread(conversationId, userId, afterInstant);
    }
}
