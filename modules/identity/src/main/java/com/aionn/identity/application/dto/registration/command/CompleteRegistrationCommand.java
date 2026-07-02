package com.aionn.identity.application.dto.registration.command;

import com.aionn.sharedkernel.application.command.Command;

public record CompleteRegistrationCommand(
                String regId,
                String password,
                String username,
                String verificationToken,
                String ipAddress,
                String userAgent) implements Command {
    @Override
    public String toString() {
        return "CompleteRegistrationCommand[regId=%s, credential=***, username=%s, verificationToken=***, ipAddress=%s, userAgent=%s]"
                .formatted(regId, username, ipAddress, userAgent);
    }
}

