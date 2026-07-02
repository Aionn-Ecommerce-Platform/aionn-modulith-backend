package com.aionn.identity.application.dto.auth.command;

import com.aionn.sharedkernel.application.command.Command;

public record RefreshTokenCommand(
                String requestRefreshToken,
                String cookieRefreshToken,
                String clientIp,
                String userAgent) implements Command {
    @Override
    public String toString() {
        return "RefreshTokenCommand[requestRefreshToken=***, cookieRefreshToken=***, clientIp=%s, userAgent=%s]"
                .formatted(clientIp, userAgent);
    }
}



