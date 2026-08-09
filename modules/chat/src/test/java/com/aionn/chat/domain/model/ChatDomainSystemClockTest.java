package com.aionn.chat.domain.model;

import com.aionn.chat.domain.exception.ChatException;
import com.aionn.chat.domain.valueobject.MessagePayload;
import com.aionn.chat.domain.valueobject.MessageStatus;
import com.aionn.chat.domain.valueobject.MessageType;
import com.aionn.chat.domain.valueobject.ParticipantRole;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatDomainSystemClockTest {

    private static Conversation conversation() {
        Conversation conversation = Conversation.start("conv-1", "buyer-1", "Buyer", null,
                "mer-1", "Shop", null, "buyer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        conversation.pullEvents();
        return conversation;
    }

    private static Message message() {
        Message message = Message.send("msg-1", "conv-1", "buyer-1", ParticipantRole.BUYER,
                MessageType.TEXT, MessagePayload.text("Hello"), List.of("mer-1"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        message.pullEvents();
        return message;
    }

    @Test
    void conversationStartWithoutClockStampsTimes() {
        Conversation conversation = conversation();

        assertThat(conversation.getCreatedAt()).isNotNull();
        assertThat(conversation.getUpdatedAt()).isNotNull();
    }

    @Test
    void conversationStartRejectsBuyerEqualToMerchant() {
        assertThatThrownBy(() -> Conversation.start("conv-2", "same", "A", null,
                "same", "B", null, "same", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(ChatException.class);
    }

    @Test
    void conversationMutatorsWithoutClockStampUpdatedAt() {
        Conversation conversation = conversation();

        conversation.joinSupport("cs-1", "CS", null, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        conversation.recordMessageSent("msg-1", MessageType.TEXT, "Hello", "buyer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        conversation.markRead("buyer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        conversation.archive("buyer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        assertThat(conversation.isArchived()).isTrue();

        conversation.unarchive("buyer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        assertThat(conversation.isArchived()).isFalse();
        assertThat(conversation.getUpdatedAt()).isNotNull();
        assertThat(conversation.participants()).hasSize(3);
    }

    @Test
    void recordMessageSentUnarchivesThread() {
        Conversation conversation = conversation();
        conversation.archive("buyer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        conversation.recordMessageSent("msg-2", MessageType.TEXT, "Hi again", "buyer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(conversation.isArchived()).isFalse();
        assertThat(conversation.getLastMessageId()).isEqualTo("msg-2");
    }

    @Test
    void unreadCountForReturnsZeroWhenNoMessages() {
        Conversation conversation = conversation();

        assertThat(conversation.unreadCountFor("buyer-1", 0, 0)).isZero();
    }

    @Test
    void unreadCountForReturnsZeroWhenAlreadyRead() {
        Conversation conversation = conversation();
        conversation.recordMessageSent("msg-1", MessageType.TEXT, "Hello", "buyer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        conversation.markRead("mer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(conversation.unreadCountFor("mer-1", 5, 5)).isZero();
    }

    @Test
    void unreadCountForReportsMessagesByOther() {
        Conversation conversation = conversation();
        conversation.recordMessageSent("msg-1", MessageType.TEXT, "Hello", "buyer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(conversation.unreadCountFor("mer-1", 5, 3)).isEqualTo(3);
    }

    @Test
    void participantLastReadMapExposesEveryParticipant() {
        Conversation conversation = conversation();

        assertThat(conversation.participantLastReadMap()).containsKeys("buyer-1", "mer-1");
    }

    @Test
    void messageSendWithoutClockStampsSentAt() {
        assertThat(message().getSentAt()).isNotNull();
    }

    @Test
    void messageMutatorsWithoutClockWork() {
        Message message = message();

        message.markDeliveredTo("mer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        assertThat(message.getStatus()).isEqualTo(MessageStatus.DELIVERED);

        message.markReadBy("mer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        assertThat(message.getStatus()).isEqualTo(MessageStatus.READ);

        message.recall("buyer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        assertThat(message.isRecalled()).isTrue();
    }

    @Test
    void markDeliveredToSenderIsIgnored() {
        Message message = message();

        message.markDeliveredTo("buyer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(message.getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(message.getDeliveredTo()).isEmpty();
    }

    @Test
    void markDeliveredIsIdempotent() {
        Message message = message();

        message.markDeliveredTo("mer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        message.pullEvents();
        message.markDeliveredTo("mer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(message.pullEvents()).isEmpty();
    }

    @Test
    void markReadBySenderIsIgnored() {
        Message message = message();

        message.markReadBy("buyer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(message.getReadBy()).isEmpty();
    }

    @Test
    void recalledMessageIgnoresFurtherDelivery() {
        Message message = message();
        message.recall("buyer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        message.markDeliveredTo("mer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        message.markReadBy("mer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(message.getDeliveredTo()).isEmpty();
        assertThat(message.previewBody()).isEqualTo("(recalled)");
    }

    @Test
    void previewBodyPerMessageType() {
        assertThat(preview(MessageType.TEXT, MessagePayload.text("Hi"))).isEqualTo("Hi");
        assertThat(preview(MessageType.SYSTEM, MessagePayload.system("Sys"))).isEqualTo("Sys");
        assertThat(preview(MessageType.IMAGE,
                new MessagePayload(null, java.util.Map.of("imageUrl", "u")))).isEqualTo("[Photo]");
        assertThat(preview(MessageType.PRODUCT_CARD,
                new MessagePayload(null, java.util.Map.of("productId", "p")))).isEqualTo("[Product]");
        assertThat(preview(MessageType.ORDER_REF,
                new MessagePayload(null, java.util.Map.of("orderId", "o")))).isEqualTo("[Order]");
    }

    @Test
    void previewBodyForTextWithoutBodyIsEmpty() {
        Message message = Message.send("msg-9", "conv-1", "buyer-1", ParticipantRole.BUYER,
                MessageType.SYSTEM, new MessagePayload(null, java.util.Map.of()), List.of("mer-1"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(message.previewBody()).isEmpty();
    }

    private static String preview(MessageType type, MessagePayload payload) {
        return Message.send("msg-x", "conv-1", "buyer-1", ParticipantRole.BUYER,
                type, payload, List.of("mer-1"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)).previewBody();
    }

    @Test
    void autoReplyCreateAndUpdateWithoutClock() {
        MerchantAutoReply autoReply = MerchantAutoReply.create("mer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        assertThat(autoReply.getCreatedAt()).isNotNull();

        autoReply.update(true, "Hi", "Away", LocalTime.of(9, 0), LocalTime.of(18, 0),
                Set.of(DayOfWeek.MONDAY), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(autoReply.isEnabled()).isTrue();
        assertThat(autoReply.getUpdatedAt()).isNotNull();
    }

    @Test
    void autoReplyUpdateWithEmptyWorkingDaysMeansAlwaysAway() {
        MerchantAutoReply autoReply = MerchantAutoReply.create("mer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        autoReply.update(true, null, "Away", null, null, Set.of(), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(autoReply.getWorkingDays()).isEmpty();
    }

    @Test
    void autoReplyUpdateKeepsHoursWhenNullPassed() {
        MerchantAutoReply autoReply = MerchantAutoReply.create("mer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        LocalTime start = autoReply.getWorkingHourStart();

        autoReply.update(true, null, "Away", null, null, null, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(autoReply.getWorkingHourStart()).isEqualTo(start);
    }
}
