package com.aionn.identity.application.usecase.kyc;

import com.aionn.identity.application.dto.kyc.result.KycDocumentResult;
import com.aionn.identity.application.mapper.KycDocumentResultMapper;
import com.aionn.identity.application.port.in.kyc.ListKycDocumentsQueryPort;
import com.aionn.identity.application.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListKycDocumentsUseCase implements ListKycDocumentsQueryPort {

    private final KycService kycService;
    private final KycDocumentResultMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<KycDocumentResult> execute(String userId, String kycId) {
        return kycService.listDocuments(userId, kycId).stream()
                .map(mapper::toResult)
                .toList();
    }
}
