package com.aionn.identity.application.dto.auth.command;

public record LinkSocialCommand(
                String userId,
                String provider,
                String providerToken) {
}



