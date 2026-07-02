package com.aionn.identity.application.dto.registration.command;

import com.aionn.sharedkernel.application.command.Command;

public record InitiateRegistrationCommand(String identity, String captchaToken, String ipAddress) implements Command {
    @Override
    public String toString() {
        return "InitiateRegistrationCommand[identity=%s, captchaToken=***, ipAddress=%s]"
                .formatted(identity, ipAddress);
    }
}



