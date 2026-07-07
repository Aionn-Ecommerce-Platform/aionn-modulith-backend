package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.media.response.UploadSignatureResponse;
import com.aionn.catalog.adapter.rest.mapper.media.CatalogMediaDtoMapper;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerId;
import com.aionn.catalog.application.port.in.media.GenerateProductMediaUploadSignatureInputPort;
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
@RequestMapping("/api/v1/catalog/media/upload-signatures")
@RequiredArgsConstructor
@Tag(name = "Catalog - Media Upload", description = "Catalog module: signed upload parameters for merchant product images")
public class CatalogMediaController {

    private final GenerateProductMediaUploadSignatureInputPort generateProductMediaUploadSignatureInputPort;
    private final CatalogMediaDtoMapper mediaDtoMapper;

    @PostMapping("/product-image")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Generate product-image upload signature", description = "Merchant-only signed upload parameters for product images, scoped to caller merchantId")
    public ResponseEntity<ApiResponse<UploadSignatureResponse>> generateProductImageSignature(
            @CurrentOwnerId String ownerId) {
        var result = generateProductMediaUploadSignatureInputPort.execute(ownerId);
        var response = mediaDtoMapper.toUploadSignatureResponse(result);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Product image upload signature generated"));
    }

    @PostMapping("/review-image")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate review-image upload signature", description = "Authenticated users get signed upload params for review evidence images")
    public ResponseEntity<ApiResponse<UploadSignatureResponse>> generateReviewImageSignature(
            @CurrentOwnerId String ownerId) {
        var result = generateProductMediaUploadSignatureInputPort.executeReview(ownerId);
        var response = mediaDtoMapper.toUploadSignatureResponse(result);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Review image upload signature generated"));
    }
}
