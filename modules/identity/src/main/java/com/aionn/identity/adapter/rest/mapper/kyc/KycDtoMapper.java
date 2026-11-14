package com.aionn.identity.adapter.rest.mapper.kyc;

import com.aionn.identity.adapter.rest.dto.kyc.request.AttachKycDocumentRequest;
import com.aionn.identity.adapter.rest.dto.kyc.request.CreateKycRequest;
import com.aionn.identity.adapter.rest.dto.kyc.response.KycAnalyticsResponse;
import com.aionn.identity.adapter.rest.dto.kyc.response.KycDocumentResponse;
import com.aionn.identity.adapter.rest.dto.kyc.response.KycResponse;
import com.aionn.identity.adapter.rest.dto.kyc.response.KycVerificationSessionResponse;
import com.aionn.identity.application.dto.analytics.result.KycAnalyticsResult;
import com.aionn.identity.application.dto.kyc.command.AttachKycDocumentCommand;
import com.aionn.identity.application.dto.kyc.command.CreateKycCommand;
import com.aionn.identity.application.dto.kyc.query.GetKycQuery;
import com.aionn.identity.application.dto.kyc.query.ListKycDocumentsQuery;
import com.aionn.identity.application.dto.kyc.command.GenerateKycVerificationSessionCommand;
import com.aionn.identity.application.dto.kyc.command.SubmitKycCommand;
import com.aionn.identity.application.dto.kyc.result.KycDocumentResult;
import com.aionn.identity.application.dto.kyc.result.KycResult;
import com.aionn.identity.application.dto.kyc.result.KycVerificationSessionResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface KycDtoMapper {

    CreateKycCommand toCreateKycCommand(String userId, CreateKycRequest request);

    GetKycQuery toGetKycQuery(String userId, String kycId);

    AttachKycDocumentCommand toAttachDocumentCommand(
            String userId,
            String kycId,
            AttachKycDocumentRequest request);

    KycResponse toResponse(KycResult result);

    List<KycResponse> toResponses(List<KycResult> results);

    KycDocumentResponse toDocumentResponse(KycDocumentResult result);

    List<KycDocumentResponse> toDocumentResponses(List<KycDocumentResult> results);

    KycVerificationSessionResponse toVerificationSessionResponse(KycVerificationSessionResult result);

    KycAnalyticsResponse toAnalyticsResponse(KycAnalyticsResult result);

    default ListKycDocumentsQuery toListDocumentsQuery(String userId, String kycId) {
        return new ListKycDocumentsQuery(userId, kycId);
    }

    default SubmitKycCommand toSubmitKycCommand(String userId, String kycId) {
        return new SubmitKycCommand(userId, kycId);
    }

    default GenerateKycVerificationSessionCommand toGenerateVerificationSessionCommand(String userId, String kycId) {
        return new GenerateKycVerificationSessionCommand(userId, kycId);
    }
}
