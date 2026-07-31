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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConversationServiceTest {

        private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
        private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

        @Mock
        private ConversationPersistencePort conversationRepository;
        @Mock
        private MessagePersistencePort messageRepository;
        @Mock
        private ChatResultMapper mapper;
        @Mock
        private EventPublisher eventPublisher;
        @Mock
        private ChatParticipantResolver participantResolver;

        private ConversationService service() {
                return new ConversationService(conversationRepository, messageRepository, mapper,
                                eventPublisher, participantResolver, CLOCK);
        }

        private static Conversation conversation() {
                Conversation conversation = Conversation.start("conv-1", "buyer-1", "Buyer", null,
                                "mer-1", "Shop", null, "buyer-1", CLOCK);
                conversation.pullEvents();
                return conversation;
        }

        private static ConversationResult resultFor(String id, long unread) {
                return new ConversationResult(id, "buyer-1", "mer-1", List.of(),
                                null, null, null, null, null, false, unread, NOW, NOW);
        }

        private void mapperEchoes() {
                when(mapper.toResult(any(Conversation.class), anyLong()))
                                .thenAnswer(invocation -> resultFor(
                                                ((Conversation) invocation.getArgument(0)).getConversationId(),
                                                invocation.getArgument(1)));
        }

        private void identityResolver() {
                when(participantResolver.resolve(anyString(), any(Conversation.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void startOrGetReturnsExistingWithoutCreating() {
                mapperEchoes();
                identityResolver();
                when(conversationRepository.findByBuyerAndMerchant("buyer-1", "mer-1"))
                                .thenReturn(Optional.of(conversation()));

                ConversationResult result = service().startOrGet(new ConversationCommands.StartConversation(
                                "buyer-1", "Buyer", null, "mer-1", "Shop", null, "buyer-1"));

                assertThat(result.conversationId()).isEqualTo("conv-1");
                verify(conversationRepository, never()).save(any());
        }

        @Test
        void startOrGetCreatesWhenAbsent() {
                mapperEchoes();
                identityResolver();
                when(conversationRepository.findByBuyerAndMerchant("buyer-1", "mer-1"))
                                .thenReturn(Optional.empty());
                when(conversationRepository.save(any(Conversation.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                service().startOrGet(new ConversationCommands.StartConversation(
                                "buyer-1", "Buyer", null, "mer-1", "Shop", null, "buyer-1"));

                verify(conversationRepository).save(any(Conversation.class));
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void markReadReturnsReceiptForBroadcasting() {
                mapperEchoes();
                identityResolver();
                Conversation conversation = conversation();
                when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
                when(conversationRepository.save(any(Conversation.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ConversationService.ReadReceipt receipt = service()
                                .markRead(new ConversationCommands.MarkRead("buyer-1", "conv-1"));

                assertThat(receipt.conversationId()).isEqualTo("conv-1");
                assertThat(receipt.participantId()).isEqualTo("buyer-1");
                assertThat(receipt.readAt()).isEqualTo(NOW);
                assertThat(conversation.participantLastReadMap()).containsEntry("buyer-1", NOW);
        }

        @Test
        void markReadRejectsNonParticipant() {
                when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation()));
                when(participantResolver.resolve(anyString(), any(Conversation.class))).thenReturn("stranger");

                ConversationService service = service();
                ConversationCommands.MarkRead command = new ConversationCommands.MarkRead("stranger", "conv-1");

                assertThatThrownBy(() -> service.markRead(command))
                                .isInstanceOf(ChatException.class)
                                .hasFieldOrPropertyWithValue("errorCode",
                                                ChatErrorCode.CONVERSATION_FORBIDDEN.getCode());
        }

        @Test
        void archiveAndUnarchiveFlipTheFlag() {
                mapperEchoes();
                identityResolver();
                Conversation conversation = conversation();
                when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
                when(conversationRepository.save(any(Conversation.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ConversationService service = service();
                service.archive(new ConversationCommands.Archive("buyer-1", "conv-1"));
                assertThat(conversation.isArchived()).isTrue();

                service.unarchive(new ConversationCommands.Unarchive("buyer-1", "conv-1"));
                assertThat(conversation.isArchived()).isFalse();
        }

        @Test
        void joinSupportAddsParticipantOnce() {
                mapperEchoes();
                Conversation conversation = conversation();
                when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
                when(conversationRepository.save(any(Conversation.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ConversationService service = service();
                service.joinSupport(new ConversationCommands.JoinSupport("cs-1", "conv-1", "CS", null));
                service.joinSupport(new ConversationCommands.JoinSupport("cs-1", "conv-1", "CS", null));

                assertThat(conversation.participants()).hasSize(3);
        }

        @Test
        void getForUserThrowsWhenMissing() {
                when(conversationRepository.findById("nope")).thenReturn(Optional.empty());

                ConversationService service = service();

                assertThatThrownBy(() -> service.getForUser("buyer-1", "nope"))
                                .isInstanceOf(ChatException.class)
                                .hasFieldOrPropertyWithValue("errorCode",
                                                ChatErrorCode.CONVERSATION_NOT_FOUND.getCode());
        }

        @Test
        void getForUserCountsUnreadSinceLastRead() {
                mapperEchoes();
                identityResolver();
                Conversation conversation = conversation();
                conversation.markRead("buyer-1", CLOCK);
                when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
                when(messageRepository.countUnread("conv-1", "buyer-1", NOW)).thenReturn(4L);

                ConversationResult result = service().getForUser("buyer-1", "conv-1");

                assertThat(result.unreadCount()).isEqualTo(4);
                verify(messageRepository).countUnread("conv-1", "buyer-1", NOW);
        }

        @Test
        void getForUserFallsBackToCreatedAtWhenNeverRead() {
                mapperEchoes();
                identityResolver();
                when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation()));
                when(messageRepository.countUnread(eq("conv-1"), eq("buyer-1"), any())).thenReturn(1L);

                service().getForUser("buyer-1", "conv-1");

                verify(messageRepository).countUnread("conv-1", "buyer-1", NOW);
        }

        @Test
        void listForUserResolvesMerchantIdForOwners() {
                mapperEchoes();
                identityResolver();
                when(participantResolver.merchantIdOrNull("owner-1")).thenReturn("mer-1");
                when(conversationRepository.findByParticipant("owner-1", "mer-1", true, 25))
                                .thenReturn(List.of(conversation()));

                assertThat(service().listForUser("owner-1", true, 25)).hasSize(1);
                verify(conversationRepository).findByParticipant("owner-1", "mer-1", true, 25);
        }

        @Test
        void getUnreadCountsAggregatesPerConversation() {
                identityResolver();
                Conversation first = conversation();
                Conversation second = Conversation.start("conv-2", "buyer-1", "Buyer", null,
                                "mer-2", "Shop2", null, "buyer-1", CLOCK);
                when(participantResolver.merchantIdOrNull("buyer-1")).thenReturn(null);
                when(conversationRepository.findByParticipant("buyer-1", null, false, 100))
                                .thenReturn(List.of(first, second));
                when(messageRepository.countUnread(eq("conv-1"), eq("buyer-1"), any())).thenReturn(3L);
                when(messageRepository.countUnread(eq("conv-2"), eq("buyer-1"), any())).thenReturn(0L);

                Map<String, Long> counts = service().getUnreadCounts("buyer-1");

                assertThat(counts).containsEntry("conv-1", 3L).containsEntry("conv-2", 0L);
        }
}
