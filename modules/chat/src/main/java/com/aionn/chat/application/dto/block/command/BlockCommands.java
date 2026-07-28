package com.aionn.chat.application.dto.block.command;

import com.aionn.sharedkernel.application.command.Command;

public final class BlockCommands {

    private BlockCommands() {
    }

    public record BlockUser(String blockerId, String blockedId, String reason) implements Command {
    }

    public record UnblockUser(String blockerId, String blockedId) implements Command {
    }
}
