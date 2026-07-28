package com.aionn.chat.application.service;

import com.aionn.chat.application.dto.block.command.BlockCommands;
import com.aionn.chat.application.port.out.UserBlockPersistencePort;
import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import com.aionn.chat.domain.model.UserBlock;
import com.aionn.sharedkernel.application.port.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBlockServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private UserBlockPersistencePort repository;
    @Mock
    private EventPublisher eventPublisher;

    private UserBlockService service() {
        return new UserBlockService(repository, eventPublisher, CLOCK);
    }

    private static UserBlock existing() {
        UserBlock block = UserBlock.block("blk-1", "blocker-1", "blocked-1", "spam", CLOCK);
        block.pullEvents();
        return block;
    }

    @Test
    void blockCreatesActiveRecord() {
        when(repository.findActive("blocker-1", "blocked-1")).thenReturn(Optional.empty());
        when(repository.save(any(UserBlock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserBlock saved = service().block(new BlockCommands.BlockUser("blocker-1", "blocked-1", "spam"));

        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
        verify(eventPublisher).publish(any(java.util.Collection.class));
    }

    @Test
    void blockIsIdempotentWhenAlreadyActive() {
        when(repository.findActive("blocker-1", "blocked-1")).thenReturn(Optional.of(existing()));

        UserBlock result = service().block(new BlockCommands.BlockUser("blocker-1", "blocked-1", "spam"));

        assertThat(result.getBlockId()).isEqualTo("blk-1");
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publish(any(java.util.Collection.class));
    }

    @Test
    void blockRejectsSelfBlock() {
        when(repository.findActive("blocker-1", "blocker-1")).thenReturn(Optional.empty());

        UserBlockService service = service();
        BlockCommands.BlockUser command = new BlockCommands.BlockUser("blocker-1", "blocker-1", null);

        assertThatThrownBy(() -> service.block(command))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.BLOCK_SELF.getCode());
    }

    @Test
    void unblockDeactivatesRecord() {
        when(repository.findActive("blocker-1", "blocked-1")).thenReturn(Optional.of(existing()));
        when(repository.save(any(UserBlock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserBlock saved = service().unblock(new BlockCommands.UnblockUser("blocker-1", "blocked-1"));

        assertThat(saved.isActive()).isFalse();
        assertThat(saved.getUpdatedAt()).isEqualTo(NOW);
        verify(eventPublisher).publish(any(java.util.Collection.class));
    }

    @Test
    void unblockThrowsWhenNoActiveBlock() {
        when(repository.findActive("blocker-1", "blocked-1")).thenReturn(Optional.empty());

        UserBlockService service = service();
        BlockCommands.UnblockUser command = new BlockCommands.UnblockUser("blocker-1", "blocked-1");

        assertThatThrownBy(() -> service.unblock(command))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.BLOCK_NOT_FOUND.getCode());
    }

    @Test
    void listMyBlocksDelegatesToRepository() {
        when(repository.findByBlocker("blocker-1")).thenReturn(List.of(existing()));

        assertThat(service().listMyBlocks("blocker-1")).hasSize(1);
    }
}
