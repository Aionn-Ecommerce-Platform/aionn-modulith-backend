package com.aionn.chat.domain.model;

import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserBlockTest {

    @Test
    void blockSelfIsRejected() {
        assertThatThrownBy(() -> UserBlock.block("blk-1", "u1", "u1", "spam"))
                .isInstanceOf(ChatException.class)
                .extracting("errorCode").isEqualTo(ChatErrorCode.BLOCK_SELF.getCode());
    }

    @Test
    void blockCreatesActiveRecordAndRecordsEvent() {
        UserBlock b = UserBlock.block("blk-1", "u1", "u2", "spam");

        assertThat(b.isActive()).isTrue();
        assertThat(b.getBlockerId()).isEqualTo("u1");
        assertThat(b.getBlockedId()).isEqualTo("u2");
        assertThat(b.getReason()).isEqualTo("spam");
        assertThat(b.peekEvents()).hasSize(1);
    }

    @Test
    void unblockMakesInactiveAndIsIdempotent() {
        UserBlock b = UserBlock.block("blk-1", "u1", "u2", "spam");
        b.pullEvents();

        b.unblock();
        b.unblock(); // idempotent

        assertThat(b.isActive()).isFalse();
        assertThat(b.peekEvents()).hasSize(1);
    }
}
