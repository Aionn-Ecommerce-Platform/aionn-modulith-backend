package com.aionn.identity.application.dto.security.command;

public record DisableMfaCommand(
                String userId,
                String password,
                String mfaCode,
                String clientIp) {
}


