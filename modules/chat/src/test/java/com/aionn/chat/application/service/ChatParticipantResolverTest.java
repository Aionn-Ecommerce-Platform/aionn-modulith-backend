package com.aionn.chat.application.service;

import com.aionn.chat.domain.model.Conversation;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatParticipantResolverTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-01T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private MerchantQueryPort merchantQueryPort;

    @InjectMocks
    private ChatParticipantResolver resolver;

    private static Conversation conversation() {
        return Conversation.start("conv-1", "buyer-1", "Buyer", null,
                "mer-1", "Shop", null, "buyer-1", CLOCK);
    }

    @Test
    void buyerIdIsUsedAsIs() {
        assertThat(resolver.resolve("buyer-1", conversation())).isEqualTo("buyer-1");
        verifyNoInteractions(merchantQueryPort);
    }

    @Test
    void merchantIdIsUsedAsIs() {
        assertThat(resolver.resolve("mer-1", conversation())).isEqualTo("mer-1");
        verifyNoInteractions(merchantQueryPort);
    }

    @Test
    void nullUserIdIsPassedThrough() {
        assertThat(resolver.resolve(null, conversation())).isNull();
        verifyNoInteractions(merchantQueryPort);
    }

    @Test
    void merchantOwnerIsSwappedForMerchantId() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));

        assertThat(resolver.resolve("owner-1", conversation())).isEqualTo("mer-1");
    }

    @Test
    void ownerOfAnotherShopKeepsTheirUserId() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-2")).thenReturn(Optional.of("mer-9"));

        assertThat(resolver.resolve("owner-2", conversation())).isEqualTo("owner-2");
    }

    @Test
    void nonMerchantKeepsTheirUserId() {
        when(merchantQueryPort.findMerchantIdByOwnerId("stranger")).thenReturn(Optional.empty());

        assertThat(resolver.resolve("stranger", conversation())).isEqualTo("stranger");
    }

    @Test
    void merchantIdOrNullReturnsResolvedMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));

        assertThat(resolver.merchantIdOrNull("owner-1")).isEqualTo("mer-1");
    }

    @Test
    void merchantIdOrNullReturnsNullForNonMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("buyer-1")).thenReturn(Optional.empty());

        assertThat(resolver.merchantIdOrNull("buyer-1")).isNull();
    }
}
