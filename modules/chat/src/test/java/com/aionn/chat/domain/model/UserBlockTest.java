package com.aionn.chat.domain.model;

import static com.aionn.sharedkernel.testing.fixture.TestClocks.FIXED;

import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserBlockTest {

    @Test
    void blockSelfIsRejected() {
        assertThatThrownBy(() -> UserBlock.block("blk-1", "u1", "u1", "spam", FIXED))
                .isInstanceOf(ChatException.class)
                .extracting("errorCode").isEqualTo(ChatErrorCode.BLOCK_SELF.getCode());
    }

    @Test
    void blockCreatesActiveRecordAndRecordsEvent() {
        UserBlock b = UserBlock.block("blk-1", "u1", "u2", "spam", FIXED);

        assertThat(b.isActive()).isTrue();
        assertThat(b.getBlockerId()).isEqualTo("u1");
        assertThat(b.getBlockedId()).isEqualTo("u2");
        assertThat(b.getReason()).isEqualTo("spam");
        assertThat(b.peekEvents()).hasSize(1);
    }

    @Test
    void unblockMakesInactiveAndIsIdempotent() {
        UserBlock b = UserBlock.block("blk-1", "u1", "u2", "spam", FIXED);
        b.pullEvents();

        b.unblock(FIXED);
        b.unblock(FIXED); // idempotent

        assertThat(b.isActive()).isFalse();
        assertThat(b.peekEvents()).hasSize(1);
    }
}
