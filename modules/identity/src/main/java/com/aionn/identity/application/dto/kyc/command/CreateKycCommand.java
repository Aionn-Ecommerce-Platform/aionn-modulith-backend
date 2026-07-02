package com.aionn.identity.application.dto.kyc.command;

import com.aionn.sharedkernel.application.command.Command;

public record CreateKycCommand(
                String userId,
                String docType) implements Command {
}



