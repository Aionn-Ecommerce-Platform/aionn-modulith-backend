package com.aionn.chat.infrastructure.persistence.adapter.message;

import com.aionn.chat.application.port.out.MessagePersistencePort;
import com.aionn.chat.domain.model.Message;
import com.aionn.chat.infrastructure.persistence.entity.MessageEntity;
import com.aionn.chat.infrastructure.persistence.mapper.MessageDomainMapper;
import com.aionn.chat.infrastructure.persistence.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MessagePersistenceAdapter implements MessagePersistencePort {

    private final MessageRepository jpa;
    private final MessageDomainMapper mapper;
    private final JdbcTemplate jdbcTemplate;

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

    @Override
    public Map<String, Long> countUnreadForConversations(Map<String, UnreadWindow> windows) {
        if (windows == null || windows.isEmpty()) {
            return Map.of();
        }
        StringBuilder sql = new StringBuilder("""
                SELECT conversation_id, COUNT(*) AS unread
                  FROM chat_messages
                 WHERE recalled = FALSE AND (
                """);
        List<Object> args = new ArrayList<>();
        boolean first = true;
        for (Map.Entry<String, UnreadWindow> entry : windows.entrySet()) {
            if (!first) {
                sql.append(" OR ");
            }
            sql.append("(conversation_id = ? AND sender_id <> ? AND sent_at > ?)");
            args.add(entry.getKey());
            args.add(entry.getValue().participantId());
            args.add(Timestamp.from(entry.getValue().since()));
            first = false;
        }
        sql.append(") GROUP BY conversation_id");

        Map<String, Long> counts = new HashMap<>();
        jdbcTemplate.query(sql.toString(), rs -> {
            counts.put(rs.getString("conversation_id"), rs.getLong("unread"));
        }, args.toArray());
        return counts;
    }
}
