package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.dto.media.response.UploadSignatureResponse;
import com.aionn.promotion.adapter.rest.mapper.media.PromotionMediaDtoMapper;
import com.aionn.promotion.application.port.in.media.GenerateBannerUploadSignatureInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/promotions/media/upload-signatures")
@RequiredArgsConstructor
@Tag(name = "Promotion - Media Upload",
        description = "Promotion module: signed upload parameters for direct admin uploads")
public class PromotionMediaController {

    private final GenerateBannerUploadSignatureInputPort generateBannerUploadSignatureInputPort;
    private final PromotionMediaDtoMapper mediaDtoMapper;

    @PostMapping("/banner")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Generate promotion-banner upload signature",
            description = "Admin-only signed upload parameters for promotion banner images")
    public ResponseEntity<ApiResponse<UploadSignatureResponse>> generateBannerSignature() {
        var result = generateBannerUploadSignatureInputPort.execute();
        var response = mediaDtoMapper.toUploadSignatureResponse(result);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Promotion banner upload signature generated"));
    }
}
