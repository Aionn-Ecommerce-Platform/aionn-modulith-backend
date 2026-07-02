package com.aionn.identity.adapter.rest.mapper.kyc;

import com.aionn.identity.adapter.rest.dto.kyc.request.AttachKycDocumentRequest;
import com.aionn.identity.adapter.rest.dto.kyc.request.CreateKycRequest;
import com.aionn.identity.adapter.rest.dto.kyc.response.KycDocumentResponse;
import com.aionn.identity.adapter.rest.dto.kyc.response.KycResponse;
import com.aionn.identity.adapter.rest.dto.kyc.response.KycVerificationSessionResponse;
import com.aionn.identity.application.dto.kyc.command.AttachKycDocumentCommand;
import com.aionn.identity.application.dto.kyc.command.CreateKycCommand;
import com.aionn.identity.application.dto.kyc.query.GetKycQuery;
import com.aionn.identity.application.dto.kyc.result.KycDocumentResult;
import com.aionn.identity.application.dto.kyc.result.KycResult;
import com.aionn.identity.application.dto.kyc.result.KycVerificationSessionResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface KycDtoMapper {

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "docType", source = "request.docType")
    CreateKycCommand toCreateKycCommand(String userId, CreateKycRequest request);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "kycId", source = "kycId")
    GetKycQuery toGetKycQuery(String userId, String kycId);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "kycId", source = "kycId")
    @Mapping(target = "documentType", source = "request.documentType")
    @Mapping(target = "url", source = "request.url")
    @Mapping(target = "publicId", source = "request.publicId")
    AttachKycDocumentCommand toAttachDocumentCommand(
            String userId,
            String kycId,
            AttachKycDocumentRequest request);

    KycResponse toResponse(KycResult result);

    List<KycResponse> toResponses(List<KycResult> results);

    KycDocumentResponse toDocumentResponse(KycDocumentResult result);

    List<KycDocumentResponse> toDocumentResponses(List<KycDocumentResult> results);

    KycVerificationSessionResponse toVerificationSessionResponse(KycVerificationSessionResult result);
}
