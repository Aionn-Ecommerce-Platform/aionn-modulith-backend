package com.aionn.identity.application.dto.auth.command;

public record UnlinkSocialCommand(
        String userId,
        String provider) {
}

