package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.dto.banner.CreateBannerRequest;
import com.aionn.promotion.adapter.rest.dto.banner.UpdateBannerRequest;
import com.aionn.promotion.adapter.rest.dto.banner.response.PromotionBannerResponse;
import com.aionn.promotion.adapter.rest.mapper.banner.PromotionBannerDtoMapper;
import com.aionn.promotion.application.port.in.banner.CreateBannerInputPort;
import com.aionn.promotion.application.port.in.banner.DeleteBannerInputPort;
import com.aionn.promotion.application.port.in.banner.GetBannerInputPort;
import com.aionn.promotion.application.port.in.banner.ListActiveBannersInputPort;
import com.aionn.promotion.application.port.in.banner.ListAllBannersInputPort;
import com.aionn.promotion.application.port.in.banner.UpdateBannerInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.adapter.web.response.PageMetadata;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions/banners")
@RequiredArgsConstructor
@Tag(name = "Promotion - Banner", description = "Public promotion banners + admin CRUD")
public class PromotionBannerController {

        private final ListActiveBannersInputPort listActiveBannersInputPort;
        private final ListAllBannersInputPort listAllBannersInputPort;
        private final GetBannerInputPort getBannerInputPort;
        private final CreateBannerInputPort createBannerInputPort;
        private final UpdateBannerInputPort updateBannerInputPort;
        private final DeleteBannerInputPort deleteBannerInputPort;
        private final PromotionBannerDtoMapper dtoMapper;

        @GetMapping
        @Operation(summary = "Get active promotion banners (public)")
        public ResponseEntity<ApiResponse<List<PromotionBannerResponse>>> getActiveBanners(
                        @RequestParam(defaultValue = "0") @Min(0) int page,
                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
                var result = listActiveBannersInputPort.execute(OffsetPagination.of(page, size));
                return ResponseEntity.ok(ApiResponse.successWithPaging(
                                dtoMapper.toResponses(result.content()),
                                PageMetadata.of(result.page(), result.size(), result.totalElements()),
                                "Promotion banners fetched"));
        }

        @GetMapping("/admin")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Admin — list all banners (active and inactive)")
        public ResponseEntity<ApiResponse<List<PromotionBannerResponse>>> listAll(
                        @RequestParam(defaultValue = "0") @Min(0) int page,
                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
                var result = listAllBannersInputPort.execute(OffsetPagination.of(page, size));
                return ResponseEntity.ok(ApiResponse.successWithPaging(
                                dtoMapper.toResponses(result.content()),
                                PageMetadata.of(result.page(), result.size(), result.totalElements()),
                                "Promotion banners fetched"));
        }

        @GetMapping("/admin/{bannerId}")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Admin — get banner by id")
        public ResponseEntity<ApiResponse<PromotionBannerResponse>> get(@PathVariable String bannerId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(getBannerInputPort.execute(bannerId)),
                                "Promotion banner fetched"));
        }

        @PostMapping
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Admin — create banner")
        public ResponseEntity<ApiResponse<PromotionBannerResponse>> create(
                        @Valid @RequestBody CreateBannerRequest request) {
                return ApiResponse.createdResponse("Promotion banner created",
                                dtoMapper.toResponse(
                                                createBannerInputPort.execute(dtoMapper.toCreateCommand(request))));
        }

        @PutMapping("/{bannerId}")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Admin — update banner")
        public ResponseEntity<ApiResponse<PromotionBannerResponse>> update(
                        @PathVariable String bannerId,
                        @Valid @RequestBody UpdateBannerRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(updateBannerInputPort.execute(
                                                dtoMapper.toUpdateCommand(bannerId, request))),
                                "Promotion banner updated"));
        }

        @DeleteMapping("/{bannerId}")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Admin — delete banner")
        public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String bannerId) {
                deleteBannerInputPort.execute(dtoMapper.toDeleteCommand(bannerId));
                return ResponseEntity.ok(ApiResponse.success("Promotion banner deleted"));
        }
}
