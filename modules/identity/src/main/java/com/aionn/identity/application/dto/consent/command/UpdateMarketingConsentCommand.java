package com.aionn.identity.application.dto.consent.command;

import com.aionn.sharedkernel.application.command.Command;

public record UpdateMarketingConsentCommand(
        String userId,
        boolean subscribed,
        String clientIp) implements Command {
}

