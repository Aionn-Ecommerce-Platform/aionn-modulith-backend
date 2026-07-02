package com.aionn.identity.application.dto.registration.command;

import com.aionn.sharedkernel.application.command.Command;

public record VerifyRegistrationOtpCommand(String regId, String otpCode) implements Command {
    @Override
    public String toString() {
        return "VerifyRegistrationOtpCommand[regId=%s, otpCode=***]".formatted(regId);
    }
}



