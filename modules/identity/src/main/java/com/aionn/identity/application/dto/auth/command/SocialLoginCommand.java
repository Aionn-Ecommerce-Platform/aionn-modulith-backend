package com.aionn.identity.application.dto.auth.command;

public record SocialLoginCommand(
                String provider,
                String providerToken,
                String ipAddress,
                String userAgent) {
}



