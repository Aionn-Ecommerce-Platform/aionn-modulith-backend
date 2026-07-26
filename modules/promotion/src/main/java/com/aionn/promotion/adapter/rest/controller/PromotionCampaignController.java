package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.dto.campaign.CancelCampaignRequest;
import com.aionn.promotion.adapter.rest.dto.campaign.ConfigureConditionRequest;
import com.aionn.promotion.adapter.rest.dto.campaign.CreateCampaignRequest;
import com.aionn.promotion.adapter.rest.dto.voucher.IssueVoucherRequest;
import com.aionn.promotion.adapter.rest.support.session.CurrentAdminId;
import com.aionn.promotion.application.dto.campaign.command.CampaignCommands;
import com.aionn.promotion.application.dto.campaign.result.CampaignResult;
import com.aionn.promotion.application.dto.voucher.result.VoucherResult;
import com.aionn.promotion.application.service.PromotionCampaignService;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions/campaigns")
@RequiredArgsConstructor
@Tag(name = "Promotion - Campaign", description = "Promotion campaign + voucher issuance")
public class PromotionCampaignController {

        private final PromotionCampaignService campaignService;

        @PostMapping
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Create campaign", description = "UC9.1")
        public ResponseEntity<ApiResponse<CampaignResult>> create(
                        @CurrentAdminId String adminId,
                        @Valid @RequestBody CreateCampaignRequest request) {
                return ApiResponse.createdResponse("Campaign created",
                                campaignService.create(new CampaignCommands.CreateCampaign(
                                                request.name(), request.type(), request.budget(), request.currency(),
                                                request.startDate(), request.endDate(), adminId)));
        }

        @PostMapping("/{campaignId}/activate")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Activate campaign", description = "Manual flash-sale start")
        public ResponseEntity<ApiResponse<CampaignResult>> activate(@PathVariable String campaignId) {
                return ResponseEntity.ok(ApiResponse.success(
                                campaignService.activate(new CampaignCommands.ActivateCampaign(campaignId)),
                                "Campaign activated"));
        }

        @PostMapping("/{campaignId}/end")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "End campaign")
        public ResponseEntity<ApiResponse<CampaignResult>> end(@PathVariable String campaignId) {
                return ResponseEntity.ok(ApiResponse.success(
                                campaignService.end(new CampaignCommands.EndCampaign(campaignId)),
                                "Campaign ended"));
        }

        @PostMapping("/{campaignId}/cancel")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Cancel campaign")
        public ResponseEntity<ApiResponse<CampaignResult>> cancel(
                        @PathVariable String campaignId,
                        @Valid @RequestBody CancelCampaignRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                campaignService.cancel(
                                                new CampaignCommands.CancelCampaign(campaignId, request.reason())),
                                "Campaign cancelled"));
        }

        @PutMapping("/{campaignId}/conditions")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Configure conditions", description = "UC9.8")
        public ResponseEntity<ApiResponse<CampaignResult>> configureCondition(
                        @PathVariable String campaignId,
                        @Valid @RequestBody ConfigureConditionRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                campaignService.configureCondition(new CampaignCommands.ConfigureCondition(
                                                campaignId, request.minOrderValue(), request.applicableCategoryIds(),
                                                request.maxClaimsPerUser(), request.maxUsesPerVoucher())),
                                "Conditions updated"));
        }

        @PostMapping("/{campaignId}/vouchers")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Issue voucher", description = "UC9.2")
        public ResponseEntity<ApiResponse<VoucherResult>> issueVoucher(
                        @PathVariable String campaignId,
                        @Valid @RequestBody IssueVoucherRequest request) {
                return ApiResponse.createdResponse("Voucher issued",
                                campaignService.issueVoucher(new CampaignCommands.IssueVoucher(
                                                campaignId, request.voucherCode(), request.discountAmount(),
                                                request.currency(),
                                                request.usageLimit(), request.validFrom(), request.validUntil())));
        }

        @GetMapping("/{campaignId}")
        @Operation(summary = "Get campaign")
        public ResponseEntity<ApiResponse<CampaignResult>> get(@PathVariable String campaignId) {
                return ResponseEntity.ok(ApiResponse.success(campaignService.get(campaignId), "Campaign fetched"));
        }

        @GetMapping
        @Operation(summary = "List campaigns", description = "Public list of promotion campaigns by status")
        public ResponseEntity<ApiResponse<List<CampaignResult>>> list(
                        @RequestParam(defaultValue = "RUNNING") String status,
                        @RequestParam(defaultValue = "50") int limit) {
                return ResponseEntity.ok(ApiResponse.success(
                                campaignService.listByStatus(status, limit),
                                "Campaigns fetched"));
        }

        @GetMapping("/{campaignId}/vouchers")
        @Operation(summary = "List campaign vouchers", description = "Get claimable vouchers of a specific campaign")
        public ResponseEntity<ApiResponse<List<VoucherResult>>> listVouchers(
                        @PathVariable String campaignId,
                        @RequestParam(defaultValue = "50") int limit) {
                return ResponseEntity.ok(ApiResponse.success(
                                campaignService.listVouchersByCampaignId(campaignId, limit),
                                "Vouchers fetched"));
        }
}
