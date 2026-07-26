package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.dto.banner.CreateBannerRequest;
import com.aionn.promotion.adapter.rest.dto.banner.UpdateBannerRequest;
import com.aionn.promotion.application.dto.banner.command.BannerCommands;
import com.aionn.promotion.application.dto.banner.result.PromotionBannerResult;
import com.aionn.promotion.application.service.PromotionBannerService;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions/banners")
@RequiredArgsConstructor
@Tag(name = "Promotion - Banner", description = "Public promotion banners + admin CRUD")
public class PromotionBannerController {

    private final PromotionBannerService bannerService;

    @GetMapping
    @Operation(summary = "Get active promotion banners (public)")
    public ResponseEntity<ApiResponse<List<PromotionBannerResult>>> getActiveBanners() {
        return ResponseEntity.ok(ApiResponse.success(
                bannerService.listActive(), "Promotion banners fetched"));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Admin — list all banners (active and inactive)")
    public ResponseEntity<ApiResponse<List<PromotionBannerResult>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success(
                bannerService.listAll(), "Promotion banners fetched"));
    }

    @GetMapping("/admin/{bannerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Admin — get banner by id")
    public ResponseEntity<ApiResponse<PromotionBannerResult>> get(@PathVariable String bannerId) {
        return ResponseEntity.ok(ApiResponse.success(
                bannerService.get(bannerId), "Promotion banner fetched"));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Admin — create banner")
    public ResponseEntity<ApiResponse<PromotionBannerResult>> create(
            @Valid @RequestBody CreateBannerRequest request) {
        boolean active = request.active() == null || request.active();
        return ApiResponse.createdResponse("Promotion banner created",
                bannerService.create(new BannerCommands.CreateBanner(
                        request.title(), request.imageUrl(), request.linkUrl(),
                        request.displayOrder(), active)));
    }

    @PutMapping("/{bannerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Admin — update banner")
    public ResponseEntity<ApiResponse<PromotionBannerResult>> update(
            @PathVariable String bannerId,
            @Valid @RequestBody UpdateBannerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                bannerService.update(new BannerCommands.UpdateBanner(
                        bannerId, request.title(), request.imageUrl(), request.linkUrl(),
                        request.displayOrder(), request.active())),
                "Promotion banner updated"));
    }

    @DeleteMapping("/{bannerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Admin — delete banner")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String bannerId) {
        bannerService.delete(new BannerCommands.DeleteBanner(bannerId));
        return ResponseEntity.ok(ApiResponse.success("Promotion banner deleted"));
    }
}
