package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.dto.campaign.CancelCampaignRequest;
import com.aionn.promotion.adapter.rest.dto.campaign.ConfigureConditionRequest;
import com.aionn.promotion.adapter.rest.dto.campaign.CreateCampaignRequest;
import com.aionn.promotion.adapter.rest.dto.campaign.response.CampaignResponse;
import com.aionn.promotion.adapter.rest.dto.voucher.IssueVoucherRequest;
import com.aionn.promotion.adapter.rest.dto.voucher.response.VoucherResponse;
import com.aionn.promotion.adapter.rest.mapper.campaign.PromotionCampaignDtoMapper;
import com.aionn.promotion.adapter.rest.mapper.voucher.VoucherDtoMapper;
import com.aionn.promotion.adapter.rest.support.session.CurrentAdminId;
import com.aionn.promotion.application.port.in.campaign.ActivateCampaignInputPort;
import com.aionn.promotion.application.port.in.campaign.CancelCampaignInputPort;
import com.aionn.promotion.application.port.in.campaign.ConfigureCampaignConditionInputPort;
import com.aionn.promotion.application.port.in.campaign.CreateCampaignInputPort;
import com.aionn.promotion.application.port.in.campaign.EndCampaignInputPort;
import com.aionn.promotion.application.port.in.campaign.GetCampaignInputPort;
import com.aionn.promotion.application.port.in.campaign.IssueCampaignVoucherInputPort;
import com.aionn.promotion.application.port.in.campaign.ListCampaignVouchersInputPort;
import com.aionn.promotion.application.port.in.campaign.ListCampaignsByStatusInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions/campaigns")
@RequiredArgsConstructor
@Tag(name = "Promotion - Campaign", description = "Promotion campaign + voucher issuance")
public class PromotionCampaignController {

        private final CreateCampaignInputPort createCampaignInputPort;
        private final ActivateCampaignInputPort activateCampaignInputPort;
        private final EndCampaignInputPort endCampaignInputPort;
        private final CancelCampaignInputPort cancelCampaignInputPort;
        private final ConfigureCampaignConditionInputPort configureCampaignConditionInputPort;
        private final IssueCampaignVoucherInputPort issueCampaignVoucherInputPort;
        private final GetCampaignInputPort getCampaignInputPort;
        private final ListCampaignsByStatusInputPort listCampaignsByStatusInputPort;
        private final ListCampaignVouchersInputPort listCampaignVouchersInputPort;
        private final PromotionCampaignDtoMapper dtoMapper;
        private final VoucherDtoMapper voucherDtoMapper;

        @PostMapping
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Create campaign")
        public ResponseEntity<ApiResponse<CampaignResponse>> create(
                        @CurrentAdminId String adminId,
                        @Valid @RequestBody CreateCampaignRequest request) {
                return ApiResponse.createdResponse("Campaign created",
                                dtoMapper.toResponse(createCampaignInputPort.execute(
                                                dtoMapper.toCreateCommand(adminId, request))));
        }

        @PostMapping("/{campaignId}/activate")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Activate campaign", description = "Manual flash-sale start")
        public ResponseEntity<ApiResponse<CampaignResponse>> activate(@PathVariable String campaignId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(activateCampaignInputPort.execute(
                                                dtoMapper.toActivateCommand(campaignId))),
                                "Campaign activated"));
        }

        @PostMapping("/{campaignId}/end")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "End campaign")
        public ResponseEntity<ApiResponse<CampaignResponse>> end(@PathVariable String campaignId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(endCampaignInputPort.execute(
                                                dtoMapper.toEndCommand(campaignId))),
                                "Campaign ended"));
        }

        @PostMapping("/{campaignId}/cancel")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Cancel campaign")
        public ResponseEntity<ApiResponse<CampaignResponse>> cancel(
                        @PathVariable String campaignId,
                        @Valid @RequestBody CancelCampaignRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(cancelCampaignInputPort.execute(
                                                dtoMapper.toCancelCommand(campaignId, request))),
                                "Campaign cancelled"));
        }

        @PutMapping("/{campaignId}/conditions")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Configure conditions")
        public ResponseEntity<ApiResponse<CampaignResponse>> configureCondition(
                        @PathVariable String campaignId,
                        @Valid @RequestBody ConfigureConditionRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(configureCampaignConditionInputPort.execute(
                                                dtoMapper.toConfigureConditionCommand(campaignId, request))),
                                "Conditions updated"));
        }

        @PostMapping("/{campaignId}/vouchers")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Issue voucher")
        public ResponseEntity<ApiResponse<VoucherResponse>> issueVoucher(
                        @PathVariable String campaignId,
                        @Valid @RequestBody IssueVoucherRequest request) {
                return ApiResponse.createdResponse("Voucher issued",
                                voucherDtoMapper.toResponse(issueCampaignVoucherInputPort.execute(
                                                dtoMapper.toIssueVoucherCommand(campaignId, request))));
        }

        @GetMapping("/{campaignId}")
        @Operation(summary = "Get campaign")
        public ResponseEntity<ApiResponse<CampaignResponse>> get(@PathVariable String campaignId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(getCampaignInputPort.execute(campaignId)),
                                "Campaign fetched"));
        }

        @GetMapping
        @Operation(summary = "List campaigns", description = "Public list of promotion campaigns by status")
        public ResponseEntity<ApiResponse<List<CampaignResponse>>> list(
                        @RequestParam(defaultValue = "RUNNING") String status,
                        @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponses(listCampaignsByStatusInputPort.execute(status, limit)),
                                "Campaigns fetched"));
        }

        @GetMapping("/{campaignId}/vouchers")
        @Operation(summary = "List campaign vouchers", description = "Get claimable vouchers of a specific campaign")
        public ResponseEntity<ApiResponse<List<VoucherResponse>>> listVouchers(
                        @PathVariable String campaignId,
                        @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
                return ResponseEntity.ok(ApiResponse.success(
                                voucherDtoMapper.toResponses(
                                                listCampaignVouchersInputPort.execute(campaignId, limit)),
                                "Vouchers fetched"));
        }
}
