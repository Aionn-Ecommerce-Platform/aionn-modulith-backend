package com.aionn.chat.application.service;

import com.aionn.chat.application.dto.conversation.command.ConversationCommands;
import com.aionn.chat.application.dto.message.command.MessageCommands;
import com.aionn.chat.application.mapper.ChatResultMapper;
import com.aionn.chat.application.port.out.RealtimeBroadcaster;
import com.aionn.chat.application.port.out.observability.ChatMetricsPort;
import com.aionn.chat.domain.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRealtimeOrchestrator {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final RealtimeBroadcaster broadcaster;
    private final ChatMetricsPort metrics;
    private final ChatResultMapper mapper;

    public Message send(MessageCommands.SendMessage command) {
        MessageService.Sent sent = messageService.persistSend(command);
        broadcaster.broadcastMessage(mapper.toResult(sent.message()), sent.recipients());
        metrics.messageSent(sent.message().getType().name());

        messageService.persistAutoReply(command.conversationId(), sent.senderRole())
                .ifPresent(reply -> {
                    broadcaster.broadcastMessage(mapper.toResult(reply.message()), reply.recipients());
                    metrics.autoReplyTriggered();
                });
        return sent.message();
    }

    public Message recall(MessageCommands.RecallMessage command) {
        Message recalled = messageService.recall(command);
        broadcaster.broadcastMessageRecalled(recalled.getConversationId(), recalled.getMessageId());
        metrics.messageRecalled();
        return recalled;
    }

    public void setTyping(MessageCommands.SetTyping command) {
        String participantId = messageService.resolveTypingParticipant(command);
        broadcaster.broadcastTypingChange(command.conversationId(), participantId, command.typing());
    }

    public ConversationService.ReadReceipt markConversationRead(ConversationCommands.MarkRead command) {
        ConversationService.ReadReceipt receipt = conversationService.markRead(command);
        broadcaster.broadcastConversationRead(receipt.conversationId(), receipt.participantId(),
                receipt.readAt());
        return receipt;
    }
}
