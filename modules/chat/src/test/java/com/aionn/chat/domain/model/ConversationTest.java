package com.aionn.chat.domain.model;

import com.aionn.chat.domain.exception.ChatException;
import com.aionn.chat.domain.valueobject.MessageType;
import com.aionn.chat.domain.valueobject.Participant;
import com.aionn.chat.domain.valueobject.ParticipantRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationTest {

    @Test
    void constructor_nullParticipants_startsEmpty() {
        Conversation conversation = new Conversation("C_1", "U_buyer", "M_seller", null,
                null, null, null, null, null, false, null, null);

        assertThat(conversation.participants()).isEmpty();
    }

    private static Conversation buyerMerchantConversation() {
        return Conversation.start("C_1",
                "U_buyer", "Buyer", null,
                "M_seller", "Seller", null,
                "U_buyer");
    }

    @Test
    void start_sameBuyerAndMerchant_throwsInvalidArgument() {
        assertThatThrownBy(() -> Conversation.start("C_1",
                "U_same", "X", null, "U_same", "Y", null, "U_same"))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CHT_900");
    }

    @Test
    void requireParticipant_nonParticipant_throwsForbidden() {
        Conversation c = buyerMerchantConversation();

        assertThatThrownBy(() -> c.requireParticipant("U_eavesdropper"))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CHT_002");
    }

    @Test
    void requireParticipant_buyerOrMerchant_returnsParticipant() {
        Conversation c = buyerMerchantConversation();

        Participant buyer = c.requireParticipant("U_buyer");
        Participant merchant = c.requireParticipant("M_seller");

        assertThat(buyer.role()).isEqualTo(ParticipantRole.BUYER);
        assertThat(merchant.role()).isEqualTo(ParticipantRole.MERCHANT);
    }

    @Test
    void recipientsExcept_excludesSenderOnly() {
        Conversation c = buyerMerchantConversation();
        c.joinSupport("U_cs", "Support", null);

        assertThat(c.recipientsExcept("U_buyer")).containsExactlyInAnyOrder("M_seller", "U_cs");
        assertThat(c.recipientsExcept("M_seller")).containsExactlyInAnyOrder("U_buyer", "U_cs");
    }

    @Test
    void joinSupport_isIdempotent() {
        Conversation c = buyerMerchantConversation();

        c.joinSupport("U_cs", "Support", null);
        c.joinSupport("U_cs", "Other", null);

        assertThat(c.participants()).hasSize(3);
        assertThat(c.recipientsExcept("U_buyer")).containsExactlyInAnyOrder("M_seller", "U_cs");
    }

    @Test
    void markRead_setsLastReadAtForParticipant() {
        Conversation c = buyerMerchantConversation();

        c.markRead("U_buyer");

        assertThat(c.participantLastReadMap().get("U_buyer")).isNotNull();
        assertThat(c.participantLastReadMap().get("M_seller")).isNull();
    }

    @Test
    void markRead_nonParticipant_throwsForbidden() {
        Conversation c = buyerMerchantConversation();

        assertThatThrownBy(() -> c.markRead("U_eavesdropper"))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CHT_002");
    }

    @Test
    void recordMessageSent_unarchivesAndUpdatesLastMessage() {
        Conversation c = buyerMerchantConversation();
        c.archive("U_buyer");
        assertThat(c.isArchived()).isTrue();

        c.recordMessageSent("M_id_1", MessageType.TEXT, "hi", "U_buyer");

        assertThat(c.isArchived()).isFalse();
        assertThat(c.getLastMessageId()).isEqualTo("M_id_1");
        assertThat(c.getLastMessageSenderId()).isEqualTo("U_buyer");
    }
}
