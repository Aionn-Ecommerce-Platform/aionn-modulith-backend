package com.aionn.chat.application.service;

import com.aionn.chat.application.dto.conversation.command.ConversationCommands;
import com.aionn.chat.application.dto.conversation.result.ConversationResult;
import com.aionn.chat.application.mapper.ChatResultMapper;
import com.aionn.chat.application.port.out.ConversationPersistencePort;
import com.aionn.chat.application.port.out.MessagePersistencePort;
import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import com.aionn.chat.domain.model.Conversation;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ConversationService {

    private static final int UNREAD_SCAN_LIMIT = 100;

    private final ConversationPersistencePort conversationRepository;
    private final MessagePersistencePort messageRepository;
    private final ChatResultMapper mapper;
    private final EventPublisher eventPublisher;
    private final ChatParticipantResolver participantResolver;
    private final Clock clock;

    public record ReadReceipt(ConversationResult conversation, String conversationId,
            String participantId, Instant readAt) {
    }

    public ConversationResult startOrGet(ConversationCommands.StartConversation command) {
        Optional<Conversation> existing = conversationRepository
                .findByBuyerAndMerchant(command.buyerId(), command.merchantId());
        if (existing.isPresent()) {
            return toResult(existing.get(), command.startedBy());
        }
        Conversation conversation = Conversation.start(IdGenerator.ulid(),
                command.buyerId(), command.buyerDisplayName(), command.buyerAvatarUrl(),
                command.merchantId(), command.merchantDisplayName(), command.merchantAvatarUrl(),
                command.startedBy(), clock);
        Conversation saved = conversationRepository.save(conversation);
        eventPublisher.publish(conversation.pullEvents());
        return toResult(saved, command.startedBy());
    }

    public ReadReceipt markRead(ConversationCommands.MarkRead command) {
        Conversation conversation = required(command.conversationId());
        String participantId = participantResolver.resolve(command.userId(), conversation);
        conversation.markRead(participantId, clock);
        Conversation saved = conversationRepository.save(conversation);
        eventPublisher.publish(conversation.pullEvents());
        return new ReadReceipt(toResult(saved, participantId), conversation.getConversationId(),
                participantId, clock.instant());
    }

    public ConversationResult archive(ConversationCommands.Archive command) {
        Conversation conversation = required(command.conversationId());
        String participantId = participantResolver.resolve(command.userId(), conversation);
        conversation.archive(participantId, clock);
        return toResult(conversationRepository.save(conversation), participantId);
    }

    public ConversationResult unarchive(ConversationCommands.Unarchive command) {
        Conversation conversation = required(command.conversationId());
        String participantId = participantResolver.resolve(command.userId(), conversation);
        conversation.unarchive(participantId, clock);
        return toResult(conversationRepository.save(conversation), participantId);
    }

    public ConversationResult joinSupport(ConversationCommands.JoinSupport command) {
        Conversation conversation = required(command.conversationId());
        conversation.joinSupport(command.supportUserId(), command.displayName(),
                command.avatarUrl(), clock);
        return toResult(conversationRepository.save(conversation), command.supportUserId());
    }

    @Transactional(readOnly = true)
    public ConversationResult getForUser(String userId, String conversationId) {
        Conversation conversation = required(conversationId);
        String participantId = participantResolver.resolve(userId, conversation);
        conversation.requireParticipant(participantId);
        return toResult(conversation, participantId);
    }

    @Transactional(readOnly = true)
    public List<ConversationResult> listForUser(String userId, boolean includeArchived, int limit) {
        return participantConversations(userId, includeArchived, limit).stream()
                .map(conversation -> toResult(conversation,
                        participantResolver.resolve(userId, conversation)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getUnreadCounts(String userId) {
        Map<String, Long> counts = new HashMap<>();
        for (Conversation conversation : participantConversations(userId, false, UNREAD_SCAN_LIMIT)) {
            String participantId = participantResolver.resolve(userId, conversation);
            counts.put(conversation.getConversationId(), unreadFor(conversation, participantId));
        }
        return counts;
    }

    private List<Conversation> participantConversations(String userId, boolean includeArchived, int limit) {
        String merchantId = participantResolver.merchantIdOrNull(userId);
        return conversationRepository.findByParticipant(userId, merchantId, includeArchived, limit);
    }

    private Conversation required(String conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CONVERSATION_NOT_FOUND));
    }

    private ConversationResult toResult(Conversation conversation, String forUserId) {
        return mapper.toResult(conversation, unreadFor(conversation, forUserId));
    }

    private long unreadFor(Conversation conversation, String participantId) {
        Instant lastRead = conversation.participantLastReadMap().get(participantId);
        Instant since = lastRead == null ? conversation.getCreatedAt() : lastRead;
        return messageRepository.countUnread(conversation.getConversationId(), participantId, since);
    }
}
