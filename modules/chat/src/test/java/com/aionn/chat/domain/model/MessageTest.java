package com.aionn.chat.domain.model;

import com.aionn.chat.domain.exception.ChatException;
import com.aionn.chat.domain.valueobject.MessagePayload;
import com.aionn.chat.domain.valueobject.MessageStatus;
import com.aionn.chat.domain.valueobject.MessageType;
import com.aionn.chat.domain.valueobject.ParticipantRole;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageTest {

    private static Message textMessage() {
        return Message.send("M_1", "C_1", "U_buyer", ParticipantRole.BUYER, MessageType.TEXT,
                MessagePayload.text("hi"), List.of("U_merchant"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
    }

    @Test
    void send_textWithEmptyBody_throwsMessageEmpty() {
        assertThatThrownBy(() -> Message.send("M_1", "C_1", "U_b", ParticipantRole.BUYER, MessageType.TEXT,
                MessagePayload.text(""), List.of("U_m"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CHT_104");
    }

    @Test
    void send_textBeyondMaxLength_throwsTooLong() {
        String tooLong = "x".repeat(Message.MAX_TEXT_LENGTH + 1);
        assertThatThrownBy(() -> Message.send("M_1", "C_1", "U_b", ParticipantRole.BUYER, MessageType.TEXT,
                MessagePayload.text(tooLong), List.of("U_m"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CHT_103");
    }

    @Test
    void markDeliveredTo_sender_isIgnored() {
        Message m = textMessage();

        m.markDeliveredTo("U_buyer", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(m.getDeliveredTo()).doesNotContain("U_buyer");
        assertThat(m.getStatus()).isEqualTo(MessageStatus.SENT);
    }

    @Test
    void markDeliveredTo_recipient_flipsStatusAndIsIdempotent() {
        Message m = textMessage();

        m.markDeliveredTo("U_merchant", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        Instant firstUpdate = m.getUpdatedAt();
        m.markDeliveredTo("U_merchant", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(m.getDeliveredTo()).containsExactly("U_merchant");
        assertThat(m.getStatus()).isEqualTo(MessageStatus.DELIVERED);
        assertThat(m.getUpdatedAt()).isEqualTo(firstUpdate);
    }

    @Test
    void markReadBy_recipient_alsoMarksDelivered() {
        Message m = textMessage();

        m.markReadBy("U_merchant", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(m.getReadBy()).containsExactly("U_merchant");
        assertThat(m.getDeliveredTo()).contains("U_merchant");
        assertThat(m.getStatus()).isEqualTo(MessageStatus.READ);
    }

    @Test
    void recall_bySender_clearsBodyAndFlipsStatus() {
        Message m = textMessage();

        m.recall("U_buyer", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(m.isRecalled()).isTrue();
        assertThat(m.getStatus()).isEqualTo(MessageStatus.RECALLED);
        assertThat(m.getPayload().body()).isEqualTo("(recalled)");
    }

    @Test
    void recall_byNonSender_throwsForbidden() {
        Message m = textMessage();

        assertThatThrownBy(() -> m.recall("U_merchant", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CHT_102");
    }

    @Test
    void recall_alreadyRecalled_throwsAlreadyRecalled() {
        Message m = textMessage();
        m.recall("U_buyer", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThatThrownBy(() -> m.recall("U_buyer", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CHT_106");
    }

    @Test
    void recall_afterWindow_throwsWindowExpired() {
        Instant longAgo = Instant.now().minus(Message.RECALL_WINDOW).minus(Duration.ofSeconds(1));
        Message expired = new Message("M_1", "C_1", "U_buyer", ParticipantRole.BUYER, MessageType.TEXT,
                MessagePayload.text("hi"), MessageStatus.SENT, new HashSet<>(), new HashSet<>(),
                false, longAgo, longAgo);

        java.time.Clock afterRecallWindow = java.time.Clock.fixed(
                longAgo.plus(Message.RECALL_WINDOW).plusSeconds(1), java.time.ZoneOffset.UTC);

        assertThatThrownBy(() -> expired.recall("U_buyer", afterRecallWindow))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CHT_105");
    }

    @Test
    void markDeliveredTo_recalledMessage_isIgnored() {
        Message m = textMessage();
        m.recall("U_buyer", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        m.markDeliveredTo("U_merchant", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(m.getDeliveredTo()).isEmpty();
    }

    @Test
    void send_nullPayload_throwsMessageEmpty() {
        assertThatThrownBy(() -> Message.send("M_1", "C_1", "U_b", ParticipantRole.BUYER, MessageType.TEXT,
                null, List.of("U_m"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CHT_104");
    }

    @Test
    void send_imageWithoutImageUrl_throwsInvalidArgument() {
        assertThatThrownBy(() -> Message.send("M_1", "C_1", "U_b", ParticipantRole.BUYER, MessageType.IMAGE,
                new MessagePayload(null, Map.of()), List.of("U_m"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CHT_900");
    }

    @Test
    void send_productCardWithoutProductId_throwsInvalidArgument() {
        assertThatThrownBy(() -> Message.send("M_1", "C_1", "U_b", ParticipantRole.BUYER,
                MessageType.PRODUCT_CARD, new MessagePayload(null, Map.of()), List.of("U_m"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CHT_900");
    }

    @Test
    void send_orderRefWithoutOrderId_throwsInvalidArgument() {
        assertThatThrownBy(() -> Message.send("M_1", "C_1", "U_b", ParticipantRole.BUYER,
                MessageType.ORDER_REF, new MessagePayload(null, Map.of()), List.of("U_m"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CHT_900");
    }

    @Test
    void constructor_nullRecipientSets_startEmpty() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Message m = new Message("M_1", "C_1", "U_buyer", ParticipantRole.BUYER, MessageType.TEXT,
                MessagePayload.text("hi"), MessageStatus.SENT, null, null, false, now, now);

        assertThat(m.getDeliveredTo()).isEmpty();
        assertThat(m.getReadBy()).isEmpty();
    }

    @Test
    void markReadBy_recalledStateKeepsRecalledStatus() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Message m = new Message("M_1", "C_1", "U_buyer", ParticipantRole.BUYER, MessageType.TEXT,
                MessagePayload.system("(recalled)"), MessageStatus.RECALLED, new HashSet<>(),
                new HashSet<>(), false, now, now);

        m.markReadBy("U_merchant", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(m.getStatus()).isEqualTo(MessageStatus.RECALLED);
        assertThat(m.getReadBy()).containsExactly("U_merchant");
    }
}
