package com.aionn.chat.application.service;

import com.aionn.chat.application.dto.conversation.command.ConversationCommands;
import com.aionn.chat.application.dto.conversation.result.ConversationResult;
import com.aionn.chat.application.dto.message.command.MessageCommands;
import com.aionn.chat.application.dto.message.result.MessageResult;
import com.aionn.chat.application.mapper.ChatResultMapper;
import com.aionn.chat.application.port.out.RealtimeBroadcaster;
import com.aionn.chat.application.port.out.observability.ChatMetricsPort;
import com.aionn.chat.domain.model.Message;
import com.aionn.chat.domain.valueobject.MessagePayload;
import com.aionn.chat.domain.valueobject.MessageType;
import com.aionn.chat.domain.valueobject.ParticipantRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatRealtimeOrchestratorTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private MessageService messageService;
    @Mock
    private ConversationService conversationService;
    @Mock
    private RealtimeBroadcaster broadcaster;
    @Mock
    private ChatMetricsPort metrics;
    @Mock
    private ChatResultMapper mapper;

    private ChatRealtimeOrchestrator orchestrator() {
        return new ChatRealtimeOrchestrator(messageService, conversationService, broadcaster,
                metrics, mapper);
    }

    private static Message message(String messageId, MessageType type) {
        Message message = Message.send(messageId, "conv-1", "buyer-1", ParticipantRole.BUYER,
                type, MessagePayload.text("Hello"), List.of("mer-1"), CLOCK);
        message.pullEvents();
        return message;
    }

    private static MessageService.Sent sent(String messageId, MessageType type) {
        return new MessageService.Sent(message(messageId, type), List.of("mer-1"), "Buyer",
                ParticipantRole.BUYER, "mer-1");
    }

    private static MessageResult result(String messageId) {
        return new MessageResult(messageId, "conv-1", "buyer-1", "BUYER", "TEXT", "Hello",
                Map.of(), "SENT", Set.of(), Set.of(), false, NOW, NOW);
    }

    private static MessageCommands.SendMessage sendCommand() {
        return new MessageCommands.SendMessage("buyer-1", "conv-1", MessageType.TEXT, "Hello", Map.of());
    }

    @Test
    void sendPersistsBeforeBroadcasting() {
        when(messageService.persistSend(any())).thenReturn(sent("msg-1", MessageType.TEXT));
        when(messageService.persistAutoReply(any(), any())).thenReturn(Optional.empty());
        when(mapper.toResult(any(Message.class))).thenReturn(result("msg-1"));

        Message returned = orchestrator().send(sendCommand());

        assertThat(returned.getMessageId()).isEqualTo("msg-1");
        InOrder order = inOrder(messageService, broadcaster);
        order.verify(messageService).persistSend(any());
        order.verify(broadcaster).broadcastMessage(any(MessageResult.class), anyList());
    }

    @Test
    void sendBroadcastsToRecipientsAndRecordsMetric() {
        when(messageService.persistSend(any())).thenReturn(sent("msg-1", MessageType.TEXT));
        when(messageService.persistAutoReply(any(), any())).thenReturn(Optional.empty());
        when(mapper.toResult(any(Message.class))).thenReturn(result("msg-1"));

        orchestrator().send(sendCommand());

        verify(broadcaster).broadcastMessage(result("msg-1"), List.of("mer-1"));
        verify(metrics).messageSent("TEXT");
    }

    @Test
    void sendDoesNotFanOutPushItself() {
        when(messageService.persistSend(any())).thenReturn(sent("msg-1", MessageType.TEXT));
        when(messageService.persistAutoReply(any(), any())).thenReturn(Optional.empty());
        when(mapper.toResult(any(Message.class))).thenReturn(result("msg-1"));

        orchestrator().send(sendCommand());

        verify(metrics, never()).pushNotificationDispatched(any());
    }

    @Test
    void sendBroadcastsAutoReplyWhenMerchantIsAway() {
        when(messageService.persistSend(any())).thenReturn(sent("msg-1", MessageType.TEXT));
        when(messageService.persistAutoReply("conv-1", ParticipantRole.BUYER))
                .thenReturn(Optional.of(new MessageService.Sent(
                        message("msg-auto", MessageType.SYSTEM), List.of("buyer-1"), "Shop",
                        ParticipantRole.MERCHANT, "mer-1")));
        when(mapper.toResult(any(Message.class))).thenReturn(result("msg-auto"));

        orchestrator().send(sendCommand());

        verify(broadcaster).broadcastMessage(result("msg-auto"), List.of("buyer-1"));
        verify(metrics).autoReplyTriggered();
    }

    @Test
    void sendSkipsAutoReplyBroadcastWhenNotApplicable() {
        when(messageService.persistSend(any())).thenReturn(sent("msg-1", MessageType.TEXT));
        when(messageService.persistAutoReply(any(), any())).thenReturn(Optional.empty());
        when(mapper.toResult(any(Message.class))).thenReturn(result("msg-1"));

        orchestrator().send(sendCommand());

        verify(metrics, never()).autoReplyTriggered();
    }

    @Test
    void recallBroadcastsRecallAndRecordsMetric() {
        when(messageService.recall(any())).thenReturn(message("msg-1", MessageType.TEXT));

        orchestrator().recall(new MessageCommands.RecallMessage("buyer-1", "msg-1"));

        verify(broadcaster).broadcastMessageRecalled("conv-1", "msg-1");
        verify(metrics).messageRecalled();
    }

    @Test
    void setTypingBroadcastsResolvedParticipant() {
        when(messageService.resolveTypingParticipant(any())).thenReturn("mer-1");

        orchestrator().setTyping(new MessageCommands.SetTyping("owner-1", "conv-1", true));

        verify(broadcaster).broadcastTypingChange("conv-1", "mer-1", true);
    }

    @Test
    void markConversationReadBroadcastsReceipt() {
        ConversationResult conversation = new ConversationResult("conv-1", "buyer-1", "mer-1",
                List.of(), null, null, null, null, null, false, 0L, NOW, NOW);
        when(conversationService.markRead(any())).thenReturn(
                new ConversationService.ReadReceipt(conversation, "conv-1", "buyer-1", NOW));

        ConversationService.ReadReceipt receipt = orchestrator()
                .markConversationRead(new ConversationCommands.MarkRead("buyer-1", "conv-1"));

        assertThat(receipt.conversation()).isEqualTo(conversation);
        verify(broadcaster).broadcastConversationRead("conv-1", "buyer-1", NOW);
    }
}
