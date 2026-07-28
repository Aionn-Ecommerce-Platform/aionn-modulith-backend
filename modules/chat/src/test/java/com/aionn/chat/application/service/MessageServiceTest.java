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
import com.aionn.chat.domain.valueobject.MessageStatus;
import com.aionn.chat.domain.valueobject.MessageType;
import com.aionn.chat.domain.valueobject.ParticipantRole;
import com.aionn.sharedkernel.application.port.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private ConversationPersistencePort conversationRepository;
    @Mock
    private MessagePersistencePort messageRepository;
    @Mock
    private UserBlockPersistencePort userBlockRepository;
    @Mock
    private MerchantAutoReplyPersistencePort autoReplyRepository;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private ChatParticipantResolver participantResolver;

    private MessageService service() {
        return new MessageService(conversationRepository, messageRepository, userBlockRepository,
                autoReplyRepository, eventPublisher, participantResolver, CLOCK);
    }

    private static Conversation conversation() {
        Conversation conversation = Conversation.start("conv-1", "buyer-1", "Buyer", null,
                "mer-1", "Shop", null, "buyer-1", CLOCK);
        conversation.pullEvents();
        return conversation;
    }

    private static MessageCommands.SendMessage sendText() {
        return new MessageCommands.SendMessage("buyer-1", "conv-1", MessageType.TEXT, "Hello", Map.of());
    }

    private void conversationFound(Conversation conversation) {
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(participantResolver.resolve(anyString(), any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void echoMessageSave() {
        when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void persistSendStoresMessageAndReturnsFanOutContext() {
        conversationFound(conversation());
        echoMessageSave();
        when(userBlockRepository.exists(anyString(), anyString())).thenReturn(false);

        MessageService.Sent sent = service().persistSend(sendText());

        assertThat(sent.message().getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(sent.message().getSentAt()).isEqualTo(NOW);
        assertThat(sent.recipients()).containsExactly("mer-1");
        assertThat(sent.senderDisplayName()).isEqualTo("Buyer");
        assertThat(sent.senderRole()).isEqualTo(ParticipantRole.BUYER);
        assertThat(sent.merchantId()).isEqualTo("mer-1");
    }

    @Test
    void persistSendUpdatesConversationLastMessage() {
        Conversation conversation = conversation();
        conversationFound(conversation);
        echoMessageSave();

        service().persistSend(sendText());

        assertThat(conversation.getLastMessagePreview()).isEqualTo("Hello");
        assertThat(conversation.getLastMessageAt()).isEqualTo(NOW);
        assertThat(conversation.getLastMessageSenderId()).isEqualTo("buyer-1");
    }

    @Test
    void persistSendRejectsWhenRecipientBlockedSender() {
        conversationFound(conversation());
        when(userBlockRepository.exists("mer-1", "buyer-1")).thenReturn(true);

        MessageService service = service();
        MessageCommands.SendMessage command = sendText();

        assertThatThrownBy(() -> service.persistSend(command))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.USER_BLOCKED.getCode());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void persistSendRejectsWhenSenderBlockedRecipient() {
        conversationFound(conversation());
        when(userBlockRepository.exists("buyer-1", "mer-1")).thenReturn(true);

        MessageService service = service();
        MessageCommands.SendMessage command = sendText();

        assertThatThrownBy(() -> service.persistSend(command))
                .isInstanceOf(ChatException.class);
    }

    @Test
    void persistSendRejectsNonParticipant() {
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation()));
        when(participantResolver.resolve(anyString(), any(Conversation.class))).thenReturn("stranger");

        MessageService service = service();
        MessageCommands.SendMessage command = new MessageCommands.SendMessage(
                "stranger", "conv-1", MessageType.TEXT, "Hi", Map.of());

        assertThatThrownBy(() -> service.persistSend(command))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CONVERSATION_FORBIDDEN.getCode());
    }

    @Test
    void persistSendRejectsMissingConversation() {
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.empty());

        MessageService service = service();
        MessageCommands.SendMessage command = sendText();

        assertThatThrownBy(() -> service.persistSend(command))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CONVERSATION_NOT_FOUND.getCode());
    }

    @Test
    void persistSendBuildsMetadataPayloadForNonTextTypes() {
        conversationFound(conversation());
        echoMessageSave();

        MessageService.Sent sent = service().persistSend(new MessageCommands.SendMessage(
                "buyer-1", "conv-1", MessageType.IMAGE, null, Map.of("imageUrl", "https://cdn/a.png")));

        assertThat(sent.message().getType()).isEqualTo(MessageType.IMAGE);
        assertThat(sent.message().previewBody()).isEqualTo("[Photo]");
    }

    private static MerchantAutoReply awayAutoReply(boolean enabled) {
        return new MerchantAutoReply("mer-1", enabled, "Hi", "We are away",
                LocalTime.of(8, 0), LocalTime.of(9, 0),
                EnumSet.of(DayOfWeek.MONDAY), ZoneId.of("UTC"), NOW, NOW);
    }

    @Test
    void persistAutoReplySkipsWhenSenderIsNotBuyer() {
        assertThat(service().persistAutoReply("conv-1", ParticipantRole.MERCHANT)).isEmpty();
        verify(conversationRepository, never()).findById(anyString());
    }

    @Test
    void persistAutoReplySkipsWhenNotConfigured() {
        conversationFound(conversation());
        when(autoReplyRepository.findByMerchantId("mer-1")).thenReturn(Optional.empty());

        assertThat(service().persistAutoReply("conv-1", ParticipantRole.BUYER)).isEmpty();
    }

    @Test
    void persistAutoReplySkipsWhenDisabled() {
        conversationFound(conversation());
        when(autoReplyRepository.findByMerchantId("mer-1"))
                .thenReturn(Optional.of(awayAutoReply(false)));

        assertThat(service().persistAutoReply("conv-1", ParticipantRole.BUYER)).isEmpty();
    }

    @Test
    void persistAutoReplySkipsDuringWorkingHours() {
        conversationFound(conversation());
        MerchantAutoReply autoReply = new MerchantAutoReply("mer-1", true, "Hi", "We are away",
                LocalTime.of(9, 0), LocalTime.of(18, 0),
                EnumSet.of(DayOfWeek.WEDNESDAY), ZoneId.of("UTC"), NOW, NOW);
        when(autoReplyRepository.findByMerchantId("mer-1")).thenReturn(Optional.of(autoReply));

        assertThat(service().persistAutoReply("conv-1", ParticipantRole.BUYER)).isEmpty();
    }

    @Test
    void persistAutoReplyStoresSystemMessageOutsideWorkingHours() {
        conversationFound(conversation());
        echoMessageSave();
        when(autoReplyRepository.findByMerchantId("mer-1"))
                .thenReturn(Optional.of(awayAutoReply(true)));

        Optional<MessageService.Sent> reply = service().persistAutoReply("conv-1", ParticipantRole.BUYER);

        assertThat(reply).isPresent();
        assertThat(reply.get().message().getType()).isEqualTo(MessageType.SYSTEM);
        assertThat(reply.get().message().getSenderId()).isEqualTo("mer-1");
        assertThat(reply.get().recipients()).containsExactly("buyer-1");
    }

    @Test
    void markDeliveredFlipsStatus() {
        Conversation conversation = conversation();
        conversationFound(conversation);
        Message message = Message.send("msg-1", "conv-1", "buyer-1", ParticipantRole.BUYER,
                MessageType.TEXT, com.aionn.chat.domain.valueobject.MessagePayload.text("Hi"),
                List.of("mer-1"), CLOCK);
        message.pullEvents();
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(message));
        echoMessageSave();

        Message saved = service().markDelivered(new MessageCommands.DeliverMessage("mer-1", "msg-1"));

        assertThat(saved.getStatus()).isEqualTo(MessageStatus.DELIVERED);
        assertThat(saved.getDeliveredTo()).contains("mer-1");
    }

    @Test
    void markReadRejectsCallerOutsideConversation() {
        Message message = Message.send("msg-1", "conv-1", "buyer-1", ParticipantRole.BUYER,
                MessageType.TEXT, com.aionn.chat.domain.valueobject.MessagePayload.text("Hi"),
                List.of("mer-1"), CLOCK);
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(message));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation()));
        when(participantResolver.resolve(anyString(), any(Conversation.class))).thenReturn("stranger");

        MessageService service = service();
        MessageCommands.ReadMessage command = new MessageCommands.ReadMessage("stranger", "msg-1");

        assertThatThrownBy(() -> service.markRead(command))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CONVERSATION_FORBIDDEN.getCode());
    }

    @Test
    void markReadAlsoMarksDelivered() {
        conversationFound(conversation());
        Message message = Message.send("msg-1", "conv-1", "buyer-1", ParticipantRole.BUYER,
                MessageType.TEXT, com.aionn.chat.domain.valueobject.MessagePayload.text("Hi"),
                List.of("mer-1"), CLOCK);
        message.pullEvents();
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(message));
        echoMessageSave();

        Message saved = service().markRead(new MessageCommands.ReadMessage("mer-1", "msg-1"));

        assertThat(saved.getStatus()).isEqualTo(MessageStatus.READ);
        assertThat(saved.getDeliveredTo()).contains("mer-1");
    }

    @Test
    void recallBySenderClearsBody() {
        conversationFound(conversation());
        Message message = Message.send("msg-1", "conv-1", "buyer-1", ParticipantRole.BUYER,
                MessageType.TEXT, com.aionn.chat.domain.valueobject.MessagePayload.text("Hi"),
                List.of("mer-1"), CLOCK);
        message.pullEvents();
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(message));
        echoMessageSave();

        Message saved = service().recall(new MessageCommands.RecallMessage("buyer-1", "msg-1"));

        assertThat(saved.isRecalled()).isTrue();
        assertThat(saved.getStatus()).isEqualTo(MessageStatus.RECALLED);
    }

    @Test
    void recallRejectsMissingMessage() {
        when(messageRepository.findById("nope")).thenReturn(Optional.empty());

        MessageService service = service();
        MessageCommands.RecallMessage command = new MessageCommands.RecallMessage("buyer-1", "nope");

        assertThatThrownBy(() -> service.recall(command))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.MESSAGE_NOT_FOUND.getCode());
    }

    @Test
    void resolveTypingParticipantValidatesMembership() {
        conversationFound(conversation());

        String pid = service().resolveTypingParticipant(
                new MessageCommands.SetTyping("buyer-1", "conv-1", true));

        assertThat(pid).isEqualTo("buyer-1");
    }

    @Test
    void getForUserReturnsMessage() {
        conversationFound(conversation());
        Message message = Message.send("msg-1", "conv-1", "buyer-1", ParticipantRole.BUYER,
                MessageType.TEXT, com.aionn.chat.domain.valueobject.MessagePayload.text("Hi"),
                List.of("mer-1"), CLOCK);
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(message));

        assertThat(service().getForUser("buyer-1", "msg-1").getMessageId()).isEqualTo("msg-1");
    }

    @Test
    void listLatestDelegatesToRepository() {
        conversationFound(conversation());
        when(messageRepository.findByConversationLatest("conv-1", 10)).thenReturn(List.of());

        assertThat(service().listLatest("buyer-1", "conv-1", 10)).isEmpty();
        verify(messageRepository).findByConversationLatest("conv-1", 10);
    }

    @Test
    void listBeforeDelegatesToRepository() {
        conversationFound(conversation());
        when(messageRepository.findByConversationBefore("conv-1", NOW, 10)).thenReturn(List.of());

        assertThat(service().listBefore("buyer-1", "conv-1", NOW, 10)).isEmpty();
        verify(messageRepository).findByConversationBefore("conv-1", NOW, 10);
    }
}
