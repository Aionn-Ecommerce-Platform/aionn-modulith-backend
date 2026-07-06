package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.merchant.AdminReasonRequest;
import com.aionn.catalog.adapter.rest.dto.merchant.RegisterMerchantRequest;
import com.aionn.catalog.adapter.rest.dto.merchant.UpdateMerchantProfileRequest;
import com.aionn.catalog.adapter.rest.support.session.CurrentAdminId;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerId;
import com.aionn.catalog.application.dto.merchant.query.GetMerchantByOwnerQuery;
import com.aionn.catalog.application.dto.merchant.query.GetMerchantQuery;
import com.aionn.catalog.application.dto.merchant.query.ListMerchantsQuery;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.port.in.merchant.ActivateMerchantInputPort;
import com.aionn.catalog.application.port.in.merchant.CloseMerchantInputPort;
import com.aionn.catalog.application.port.in.merchant.GetMerchantByOwnerInputPort;
import com.aionn.catalog.application.port.in.merchant.GetMerchantInputPort;
import com.aionn.catalog.application.port.in.merchant.ListMerchantsInputPort;
import com.aionn.catalog.application.port.in.merchant.RegisterMerchantInputPort;
import com.aionn.catalog.application.port.in.merchant.SuspendMerchantInputPort;
import com.aionn.catalog.application.port.in.merchant.UpdateMerchantProfileInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.aionn.catalog.adapter.rest.mapper.merchant.MerchantDtoMapper;
import com.aionn.catalog.application.port.in.merchant.UpdateGlobalCommissionRateInputPort;
import com.aionn.catalog.application.port.in.merchant.UpdateMerchantCommissionRateInputPort;
import com.aionn.catalog.adapter.rest.dto.merchant.UpdateCommissionRateRequest;
import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.sharedkernel.adapter.web.response.PageMetadata;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/merchants")
@RequiredArgsConstructor
@Tag(name = "Catalog - Merchant", description = "Merchant storefront lifecycle endpoints")
public class MerchantController {

        private final RegisterMerchantInputPort registerMerchantInputPort;
        private final UpdateMerchantProfileInputPort updateMerchantProfileInputPort;
        private final SuspendMerchantInputPort suspendMerchantInputPort;
        private final ActivateMerchantInputPort activateMerchantInputPort;
        private final CloseMerchantInputPort closeMerchantInputPort;
        private final GetMerchantByOwnerInputPort getMerchantByOwnerInputPort;
        private final GetMerchantInputPort getMerchantInputPort;
        private final ListMerchantsInputPort listMerchantsInputPort;
        private final MerchantDtoMapper merchantDtoMapper;
        private final UpdateGlobalCommissionRateInputPort updateGlobalCommissionRateInputPort;
        private final UpdateMerchantCommissionRateInputPort updateMerchantCommissionRateInputPort;

        @PostMapping
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Register merchant", description = "Bootstrap a merchant storefront for the authenticated seller")
        public ResponseEntity<ApiResponse<MerchantResult>> register(
                        @CurrentOwnerId String ownerId,
                        @Valid @RequestBody RegisterMerchantRequest request) {
                MerchantResult result = registerMerchantInputPort.execute(
                                merchantDtoMapper.toRegisterMerchantCommand(ownerId, request));
                return ApiResponse.createdResponse("Merchant registered", result);
        }

        @PutMapping("/{merchantId}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Update merchant profile", description = "Update merchant display name, logo, description")
        public ResponseEntity<ApiResponse<MerchantResult>> updateProfile(
                        @CurrentOwnerId String ownerId,
                        @PathVariable String merchantId,
                        @Valid @RequestBody UpdateMerchantProfileRequest request) {
                MerchantResult result = updateMerchantProfileInputPort.execute(
                                merchantDtoMapper.toUpdateMerchantProfileCommand(merchantId, ownerId, request));
                return ResponseEntity.ok(ApiResponse.success(result, "Merchant profile updated"));
        }

        @PostMapping("/{merchantId}/suspend")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Suspend merchant", description = "Admin temporarily disables a storefront")
        public ResponseEntity<ApiResponse<MerchantResult>> suspend(
                        @CurrentAdminId String adminId,
                        @PathVariable String merchantId,
                        @Valid @RequestBody AdminReasonRequest request) {
                MerchantResult result = suspendMerchantInputPort.execute(
                                merchantDtoMapper.toSuspendMerchantCommand(merchantId, adminId, request));
                return ResponseEntity.ok(ApiResponse.success(result, "Merchant suspended"));
        }

        @PostMapping("/{merchantId}/activate")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Activate merchant", description = "Admin restores a previously suspended storefront")
        public ResponseEntity<ApiResponse<MerchantResult>> activate(
                        @CurrentAdminId String adminId,
                        @PathVariable String merchantId,
                        @Valid @RequestBody AdminReasonRequest request) {
                MerchantResult result = activateMerchantInputPort.execute(
                                merchantDtoMapper.toActivateMerchantCommand(merchantId, adminId, request));
                return ResponseEntity.ok(ApiResponse.success(result, "Merchant activated"));
        }

        @PostMapping("/{merchantId}/close")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Close merchant", description = "Permanently close the storefront once open orders are settled")
        public ResponseEntity<ApiResponse<MerchantResult>> close(
                        @CurrentOwnerId String ownerId,
                        @PathVariable String merchantId,
                        @Valid @RequestBody AdminReasonRequest request) {
                MerchantResult result = closeMerchantInputPort.execute(
                                merchantDtoMapper.toCloseMerchantCommand(merchantId, ownerId, request));
                return ResponseEntity.ok(ApiResponse.success(result, "Merchant closed"));
        }

        @GetMapping("/me")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get my merchant", description = "Resolve the merchant storefront owned by the authenticated user")
        public ResponseEntity<ApiResponse<MerchantResult>> getMine(@CurrentOwnerId String ownerId) {
                return ResponseEntity.ok(ApiResponse.success(
                                getMerchantByOwnerInputPort.execute(new GetMerchantByOwnerQuery(ownerId)),
                                "Merchant fetched"));
        }

        @GetMapping("/{merchantId}")
        @Operation(summary = "Get merchant", description = "Public read of merchant storefront")
        public ResponseEntity<ApiResponse<MerchantResult>> get(@PathVariable String merchantId) {
                return ResponseEntity.ok(ApiResponse.success(
                                getMerchantInputPort.execute(new GetMerchantQuery(merchantId)), "Merchant fetched"));
        }

        @GetMapping
        @Operation(summary = "List merchants", description = "Public list of all merchants on the platform")
        public ResponseEntity<ApiResponse<List<MerchantResult>>> list(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                PageResult<MerchantResult> results = listMerchantsInputPort.execute(new ListMerchantsQuery(OffsetPagination.of(page, size)));
                return ResponseEntity.ok(ApiResponse.successWithPaging(
                                results.content(),
                                PageMetadata.of(results.page(), results.size(), results.totalElements()),
                                "Merchants listed"));
        }

        @PutMapping("/settings/commission-rate")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Update global default commission rate")
        public ResponseEntity<ApiResponse<Void>> updateGlobalCommissionRate(
                        @Valid @RequestBody UpdateCommissionRateRequest request) {
                updateGlobalCommissionRateInputPort.execute(merchantDtoMapper.toUpdateGlobalCommissionRateCommand(request));
                return ResponseEntity.ok(ApiResponse.success("Global default commission rate updated"));
        }

        @PutMapping("/{merchantId}/commission-rate")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Update merchant commission rate")
        public ResponseEntity<ApiResponse<MerchantResult>> updateMerchantCommissionRate(
                        @PathVariable String merchantId,
                        @Valid @RequestBody UpdateCommissionRateRequest request) {
                MerchantResult result = updateMerchantCommissionRateInputPort.execute(
                                merchantDtoMapper.toUpdateMerchantCommissionRateCommand(merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(result, "Merchant commission rate updated"));
        }
}
