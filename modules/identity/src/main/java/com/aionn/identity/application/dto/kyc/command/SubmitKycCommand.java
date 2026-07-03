package com.aionn.identity.application.dto.kyc.command;

import com.aionn.sharedkernel.application.command.Command;

public record SubmitKycCommand(
        String userId,
        String kycId) implements Command {
}
