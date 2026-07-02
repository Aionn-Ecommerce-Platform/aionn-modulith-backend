package com.aionn.identity.application.dto.auth.command;

import com.aionn.sharedkernel.application.command.Command;

public record LinkSocialCommand(
                String userId,
                String provider,
                String providerToken) implements Command {
    @Override
    public String toString() {
        return "LinkSocialCommand[userId=%s, provider=%s, providerToken=***]".formatted(userId, provider);
    }
}



