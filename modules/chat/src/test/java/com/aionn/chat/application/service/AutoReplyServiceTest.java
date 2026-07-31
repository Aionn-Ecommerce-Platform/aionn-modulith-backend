package com.aionn.chat.application.service;

import com.aionn.chat.application.dto.autoreply.command.AutoReplyCommands;
import com.aionn.chat.application.port.out.MerchantAutoReplyPersistencePort;
import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import com.aionn.chat.domain.model.MerchantAutoReply;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoReplyServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private MerchantAutoReplyPersistencePort repository;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private MerchantQueryPort merchantQueryPort;

    private AutoReplyService service() {
        return new AutoReplyService(repository, eventPublisher, merchantQueryPort, CLOCK);
    }

    private static AutoReplyCommands.UpdateAutoReply updateCommand(String ownerId, String merchantId) {
        return new AutoReplyCommands.UpdateAutoReply(ownerId, merchantId, true, "Hi", "Away",
                LocalTime.of(9, 0), LocalTime.of(18, 0), Set.of(DayOfWeek.MONDAY));
    }

    @Test
    void getRejectsCallerWithoutMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.empty());

        AutoReplyService service = service();

        assertThatThrownBy(() -> service.get("owner-1", "mer-1"))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.AUTO_REPLY_FORBIDDEN.getCode());
    }

    @Test
    void getRejectsCallerOwningAnotherMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-9"));

        AutoReplyService service = service();

        assertThatThrownBy(() -> service.get("owner-1", "mer-1"))
                .isInstanceOf(ChatException.class);
    }

    @Test
    void getReturnsDefaultsWhenNotConfigured() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(repository.findByMerchantId("mer-1")).thenReturn(Optional.empty());

        MerchantAutoReply autoReply = service().get("owner-1", "mer-1");

        assertThat(autoReply.getMerchantId()).isEqualTo("mer-1");
        assertThat(autoReply.isEnabled()).isFalse();
        assertThat(autoReply.getCreatedAt()).isEqualTo(NOW);
        verify(repository, never()).save(any());
    }

    @Test
    void getReturnsStoredConfig() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(repository.findByMerchantId("mer-1"))
                .thenReturn(Optional.of(MerchantAutoReply.create("mer-1", CLOCK)));

        assertThat(service().get("owner-1", "mer-1").getMerchantId()).isEqualTo("mer-1");
    }

    @Test
    void updateRejectsCallerOwningAnotherMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-9"));

        AutoReplyService service = service();
        AutoReplyCommands.UpdateAutoReply command = updateCommand("owner-1", "mer-1");

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(ChatException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updatePersistsAndPublishesEvent() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(repository.findByMerchantId("mer-1")).thenReturn(Optional.empty());
        when(repository.save(any(MerchantAutoReply.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MerchantAutoReply saved = service().update(updateCommand("owner-1", "mer-1"));

        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getAwayMessage()).isEqualTo("Away");
        assertThat(saved.getWorkingDays()).containsExactly(DayOfWeek.MONDAY);
        assertThat(saved.getUpdatedAt()).isEqualTo(NOW);
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void updateRejectsInvertedWorkingHours() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(repository.findByMerchantId("mer-1")).thenReturn(Optional.empty());

        AutoReplyService service = service();
        AutoReplyCommands.UpdateAutoReply command = new AutoReplyCommands.UpdateAutoReply(
                "owner-1", "mer-1", true, "Hi", "Away",
                LocalTime.of(18, 0), LocalTime.of(9, 0), Set.of(DayOfWeek.MONDAY));

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(ChatException.class);
    }
}
