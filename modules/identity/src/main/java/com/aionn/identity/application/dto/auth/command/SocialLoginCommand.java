package com.aionn.identity.application.dto.auth.command;

import com.aionn.sharedkernel.application.command.Command;

public record SocialLoginCommand(
                String provider,
                String providerToken,
                String ipAddress,
                String userAgent) implements Command {
    @Override
    public String toString() {
        return "SocialLoginCommand[provider=%s, providerToken=***, ipAddress=%s, userAgent=%s]"
                .formatted(provider, ipAddress, userAgent);
    }
}



