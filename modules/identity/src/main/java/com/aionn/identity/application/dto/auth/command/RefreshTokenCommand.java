package com.aionn.identity.application.dto.auth.command;

public record RefreshTokenCommand(
                String requestRefreshToken,
                String cookieRefreshToken,
                String clientIp,
                String userAgent) {
}



