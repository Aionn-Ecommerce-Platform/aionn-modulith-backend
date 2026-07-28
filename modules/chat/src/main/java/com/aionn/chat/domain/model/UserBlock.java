package com.aionn.chat.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.chat.domain.event.ChatEvents;
import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

@Getter
public class UserBlock extends AggregateRoot {

    private final String blockId;
    private final String blockerId;
    private final String blockedId;
    private final String reason;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public UserBlock(String blockId, String blockerId, String blockedId, String reason,
            boolean active, Instant createdAt, Instant updatedAt) {
        this.blockId = blockId;
        this.blockerId = blockerId;
        this.blockedId = blockedId;
        this.reason = reason;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserBlock block(String blockId, String blockerId, String blockedId, String reason) {
        return block(blockId, blockerId, blockedId, reason, Clock.systemUTC());
    }

    public static UserBlock block(String blockId, String blockerId, String blockedId, String reason,
            Clock clock) {
        Guard.require(!blockerId.equals(blockedId),
                () -> new ChatException(ChatErrorCode.BLOCK_SELF));
        Instant now = clock.instant();
        UserBlock b = new UserBlock(blockId, blockerId, blockedId, reason, true, now, now);
        b.registerEvent(new ChatEvents.UserBlocked(blockerId, blockedId, now));
        return b;
    }

    public void unblock() {
        unblock(Clock.systemUTC());
    }

    public void unblock(Clock clock) {
        if (!active)
            return;
        this.active = false;
        Instant now = clock.instant();
        this.updatedAt = now;
        registerEvent(new ChatEvents.UserUnblocked(blockerId, blockedId, now));
    }

    @Override
    protected String aggregateId() {
        return blockId;
    }
}
