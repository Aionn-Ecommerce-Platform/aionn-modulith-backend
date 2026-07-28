package com.aionn.chat.application.port.out;

import com.aionn.chat.domain.model.Message;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MessagePersistencePort {

    Message save(Message message);

    Optional<Message> findById(String messageId);

List<Message> findByConversationBefore(String conversationId, Instant before, int limit);

List<Message> findByConversationLatest(String conversationId, int limit);

long countUnread(String conversationId, String userId, Instant afterInstant);
}

