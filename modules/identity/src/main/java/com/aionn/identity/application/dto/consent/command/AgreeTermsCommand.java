package com.aionn.identity.application.dto.consent.command;

import com.aionn.sharedkernel.application.command.Command;

public record AgreeTermsCommand(
        String userId,
        String version,
        String clientIp) implements Command {
}

