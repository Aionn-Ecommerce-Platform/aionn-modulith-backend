package com.aionn.identity.application.usecase.kyc;

import com.aionn.identity.application.dto.kyc.command.AttachKycDocumentCommand;
import com.aionn.identity.application.dto.kyc.result.KycDocumentResult;
import com.aionn.identity.application.mapper.KycDocumentResultMapper;
import com.aionn.identity.application.port.in.kyc.AttachKycDocumentInputPort;
import com.aionn.identity.application.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttachKycDocumentUseCase implements AttachKycDocumentInputPort {

    private final KycService kycService;
    private final KycDocumentResultMapper mapper;

    @Override
    @Transactional
    public KycDocumentResult execute(AttachKycDocumentCommand command) {
        return mapper.toResult(kycService.attachDocument(
                command.userId(),
                command.kycId(),
                command.documentType(),
                command.url(),
                command.publicId()));
    }
}
