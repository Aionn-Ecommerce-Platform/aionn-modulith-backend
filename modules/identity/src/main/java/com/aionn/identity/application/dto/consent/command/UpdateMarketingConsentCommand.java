package com.aionn.identity.application.dto.consent.command;

public record UpdateMarketingConsentCommand(
        String userId,
        boolean subscribed,
        String clientIp) {
}

