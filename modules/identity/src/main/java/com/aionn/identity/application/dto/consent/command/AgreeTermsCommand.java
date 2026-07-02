package com.aionn.identity.application.dto.consent.command;

public record AgreeTermsCommand(
        String userId,
        String version,
        String clientIp) {
}

