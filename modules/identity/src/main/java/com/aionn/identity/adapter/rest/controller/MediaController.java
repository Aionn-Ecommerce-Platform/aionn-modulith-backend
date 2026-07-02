package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.media.response.UploadSignatureResponse;
import com.aionn.identity.adapter.rest.mapper.media.MediaDtoMapper;
import com.aionn.identity.application.port.in.media.GenerateAvatarUploadSignatureInputPort;
import com.aionn.identity.application.port.in.media.GenerateKycDocumentUploadSignatureInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/media/upload-signatures")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Identity - Media Upload", description = "Identity module: generate signed upload parameters for direct client uploads")
public class MediaController {

    private final GenerateAvatarUploadSignatureInputPort generateAvatarUploadSignatureInputPort;
    private final GenerateKycDocumentUploadSignatureInputPort generateKycDocumentUploadSignatureInputPort;
    private final MediaDtoMapper mediaDtoMapper;

    @PostMapping("/avatar")
    @Operation(summary = "Generate avatar upload signature", description = "Generate signed upload parameters for avatar upload")
    public ResponseEntity<ApiResponse<UploadSignatureResponse>> generateAvatarSignature(
            Authentication authentication) {
        var result = generateAvatarUploadSignatureInputPort.execute(authentication.getName());
        var response = mediaDtoMapper.toUploadSignatureResponse(result);
        return ResponseEntity.ok(ApiResponse.success(response, "Avatar upload signature generated"));
    }

    @PostMapping("/kyc")
    @Operation(summary = "Generate KYC document upload signature", description = "Generate signed upload parameters for KYC document upload")
    public ResponseEntity<ApiResponse<UploadSignatureResponse>> generateKycDocumentSignature(
            Authentication authentication) {
        var result = generateKycDocumentUploadSignatureInputPort.execute(authentication.getName());
        var response = mediaDtoMapper.toUploadSignatureResponse(result);
        return ResponseEntity.ok(ApiResponse.success(response, "KYC document upload signature generated"));
    }
}
