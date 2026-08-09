package com.aionn.chat.domain.model;

import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserBlockTest {

    @Test
    void blockSelfIsRejected() {
        assertThatThrownBy(() -> UserBlock.block("blk-1", "u1", "u1", "spam", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(ChatException.class)
                .extracting("errorCode").isEqualTo(ChatErrorCode.BLOCK_SELF.getCode());
    }

    @Test
    void blockCreatesActiveRecordAndRecordsEvent() {
        UserBlock b = UserBlock.block("blk-1", "u1", "u2", "spam", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(b.isActive()).isTrue();
        assertThat(b.getBlockerId()).isEqualTo("u1");
        assertThat(b.getBlockedId()).isEqualTo("u2");
        assertThat(b.getReason()).isEqualTo("spam");
        assertThat(b.peekEvents()).hasSize(1);
    }

    @Test
    void unblockMakesInactiveAndIsIdempotent() {
        UserBlock b = UserBlock.block("blk-1", "u1", "u2", "spam", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        b.pullEvents();

        b.unblock(java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        b.unblock(java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)); // idempotent

        assertThat(b.isActive()).isFalse();
        assertThat(b.peekEvents()).hasSize(1);
    }
}
