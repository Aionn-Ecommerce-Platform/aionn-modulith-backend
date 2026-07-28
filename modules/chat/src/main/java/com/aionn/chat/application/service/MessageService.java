package com.aionn.chat.application.service;

import com.aionn.chat.application.dto.message.command.MessageCommands;
import com.aionn.chat.application.port.out.ConversationPersistencePort;
import com.aionn.chat.application.port.out.MerchantAutoReplyPersistencePort;
import com.aionn.chat.application.port.out.MessagePersistencePort;
import com.aionn.chat.application.port.out.UserBlockPersistencePort;
import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import com.aionn.chat.domain.model.Conversation;
import com.aionn.chat.domain.model.MerchantAutoReply;
import com.aionn.chat.domain.model.Message;
import com.aionn.chat.domain.valueobject.MessagePayload;
import com.aionn.chat.domain.valueobject.MessageType;
import com.aionn.chat.domain.valueobject.Participant;
import com.aionn.chat.domain.valueobject.ParticipantRole;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    private final ConversationPersistencePort conversationRepository;
    private final MessagePersistencePort messageRepository;
    private final UserBlockPersistencePort userBlockRepository;
    private final MerchantAutoReplyPersistencePort autoReplyRepository;
    private final EventPublisher eventPublisher;
    private final ChatParticipantResolver participantResolver;
    private final Clock clock;

    public record Sent(Message message, List<String> recipients, String senderDisplayName,
            ParticipantRole senderRole, String merchantId) {
    }

    public Sent persistSend(MessageCommands.SendMessage command) {
        Conversation conversation = requiredConversation(command.conversationId());
        String senderPid = participantResolver.resolve(command.senderId(), conversation);
        Participant sender = conversation.requireParticipant(senderPid);

        List<String> recipients = conversation.recipientsExcept(senderPid);
        for (String recipientId : recipients) {
            if (userBlockRepository.exists(recipientId, senderPid)
                    || userBlockRepository.exists(senderPid, recipientId)) {
                throw new ChatException(ChatErrorCode.USER_BLOCKED);
            }
        }

        Message message = Message.send(IdGenerator.ulid(), conversation.getConversationId(),
                senderPid, sender.role(), command.type(), buildPayload(command), recipients, clock);
        conversation.recordMessageSent(message.getMessageId(), command.type(),
                message.previewBody(), senderPid, clock);

        Message savedMessage = messageRepository.save(message);
        conversationRepository.save(conversation);
        eventPublisher.publish(message.pullEvents());
        eventPublisher.publish(conversation.pullEvents());

        String senderDisplayName = sender.displayName() == null ? sender.userId() : sender.displayName();
        return new Sent(savedMessage, recipients, senderDisplayName, sender.role(),
                conversation.getMerchantId());
    }

    public Optional<Sent> persistAutoReply(String conversationId, ParticipantRole senderRole) {
        if (senderRole != ParticipantRole.BUYER) {
            return Optional.empty();
        }
        Conversation conversation = requiredConversation(conversationId);
        Optional<MerchantAutoReply> configured = autoReplyRepository
                .findByMerchantId(conversation.getMerchantId());
        if (configured.isEmpty()) {
            return Optional.empty();
        }
        MerchantAutoReply autoReply = configured.get();
        if (!autoReply.isEnabled() || autoReply.isWithinWorkingHours(clock.instant())) {
            return Optional.empty();
        }

        String merchantId = conversation.getMerchantId();
        Participant merchant = conversation.requireParticipant(merchantId);
        List<String> recipients = conversation.recipientsExcept(merchantId);
        Message reply = Message.send(IdGenerator.ulid(), conversationId, merchantId,
                merchant.role(), MessageType.SYSTEM,
                MessagePayload.system(autoReply.getAwayMessage()), recipients, clock);
        conversation.recordMessageSent(reply.getMessageId(), MessageType.SYSTEM,
                reply.previewBody(), merchantId, clock);

        Message savedReply = messageRepository.save(reply);
        conversationRepository.save(conversation);
        eventPublisher.publish(reply.pullEvents());
        eventPublisher.publish(conversation.pullEvents());

        String displayName = merchant.displayName() == null ? merchant.userId() : merchant.displayName();
        return Optional.of(new Sent(savedReply, recipients, displayName, merchant.role(), merchantId));
    }

    public Message markDelivered(MessageCommands.DeliverMessage command) {
        Message message = required(command.messageId());
        message.markDeliveredTo(participantOf(message, command.userId()), clock);
        Message saved = messageRepository.save(message);
        eventPublisher.publish(message.pullEvents());
        return saved;
    }

    public Message markRead(MessageCommands.ReadMessage command) {
        Message message = required(command.messageId());
        message.markReadBy(participantOf(message, command.userId()), clock);
        Message saved = messageRepository.save(message);
        eventPublisher.publish(message.pullEvents());
        return saved;
    }

    public Message recall(MessageCommands.RecallMessage command) {
        Message message = required(command.messageId());
        message.recall(participantOf(message, command.userId()), clock);
        Message saved = messageRepository.save(message);
        eventPublisher.publish(message.pullEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public String resolveTypingParticipant(MessageCommands.SetTyping command) {
        Conversation conversation = requiredConversation(command.conversationId());
        String pid = participantResolver.resolve(command.userId(), conversation);
        conversation.requireParticipant(pid);
        return pid;
    }

    @Transactional(readOnly = true)
    public Message getForUser(String userId, String messageId) {
        Message message = required(messageId);
        participantOf(message, userId);
        return message;
    }

    @Transactional(readOnly = true)
    public List<Message> listLatest(String userId, String conversationId, int limit) {
        requireParticipantIn(conversationId, userId);
        return messageRepository.findByConversationLatest(conversationId, limit);
    }

    @Transactional(readOnly = true)
    public List<Message> listBefore(String userId, String conversationId, Instant before, int limit) {
        requireParticipantIn(conversationId, userId);
        return messageRepository.findByConversationBefore(conversationId, before, limit);
    }

    private static MessagePayload buildPayload(MessageCommands.SendMessage command) {
        return switch (command.type()) {
            case TEXT -> MessagePayload.text(command.body());
            case SYSTEM -> MessagePayload.system(command.body());
            default -> new MessagePayload(command.body(),
                    command.metadata() == null ? Map.of() : command.metadata());
        };
    }

    private Message required(String messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.MESSAGE_NOT_FOUND));
    }

    private Conversation requiredConversation(String conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.CONVERSATION_NOT_FOUND));
    }

    private String participantOf(Message message, String userId) {
        return requireParticipantIn(message.getConversationId(), userId);
    }

    private String requireParticipantIn(String conversationId, String userId) {
        Conversation conversation = requiredConversation(conversationId);
        String pid = participantResolver.resolve(userId, conversation);
        conversation.requireParticipant(pid);
        return pid;
    }
}
