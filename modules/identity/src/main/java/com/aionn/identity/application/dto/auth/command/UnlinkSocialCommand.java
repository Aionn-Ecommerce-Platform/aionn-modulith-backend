package com.aionn.identity.application.dto.auth.command;

import com.aionn.sharedkernel.application.command.Command;

public record UnlinkSocialCommand(
        String userId,
        String provider) implements Command {
}

