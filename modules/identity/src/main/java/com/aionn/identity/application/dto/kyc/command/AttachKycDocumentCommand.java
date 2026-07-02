package com.aionn.identity.application.dto.kyc.command;

import com.aionn.sharedkernel.application.command.Command;

public record AttachKycDocumentCommand(
        String userId,
        String kycId,
        String documentType,
        String url,
        String publicId) implements Command {
}
