package com.aionn.identity.application.port.in.kyc;

import com.aionn.identity.application.dto.kyc.command.AttachKycDocumentCommand;
import com.aionn.identity.application.dto.kyc.result.KycDocumentResult;

public interface AttachKycDocumentInputPort {
    KycDocumentResult execute(AttachKycDocumentCommand command);
}
