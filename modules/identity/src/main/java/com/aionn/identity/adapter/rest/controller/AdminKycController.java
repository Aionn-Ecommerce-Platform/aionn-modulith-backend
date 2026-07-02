package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.kyc.request.AdminApproveKycRequest;
import com.aionn.identity.adapter.rest.dto.kyc.request.AdminMarkInReviewKycRequest;
import com.aionn.identity.adapter.rest.dto.kyc.request.AdminRejectKycRequest;
import com.aionn.identity.adapter.rest.dto.kyc.response.KycResponse;
import com.aionn.identity.adapter.rest.mapper.kyc.KycDtoMapper;
import com.aionn.identity.application.dto.analytics.result.KycAnalyticsResult;
import com.aionn.identity.application.dto.kyc.command.KycAdminCommands;
import com.aionn.identity.application.port.in.kyc.ApproveKycInputPort;
import com.aionn.identity.application.port.in.kyc.GetAdminKycQueryPort;
import com.aionn.identity.application.port.in.kyc.GetKycAnalyticsQueryPort;
import com.aionn.identity.application.port.in.kyc.ListAdminKycQueryPort;
import com.aionn.identity.application.port.in.kyc.MarkKycInReviewInputPort;
import com.aionn.identity.application.port.in.kyc.RejectKycInputPort;
import com.aionn.identity.domain.valueobject.KycStatus;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/kyc")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
@Tag(name = "Identity - KYC Admin",
        description = "Admin review and decision endpoints for KYC profiles")
public class AdminKycController {

    private final ListAdminKycQueryPort listAdminKycQueryPort;
    private final GetAdminKycQueryPort getAdminKycQueryPort;
    private final MarkKycInReviewInputPort markKycInReviewInputPort;
    private final ApproveKycInputPort approveKycInputPort;
    private final RejectKycInputPort rejectKycInputPort;
    private final GetKycAnalyticsQueryPort getKycAnalyticsQueryPort;
    private final KycDtoMapper kycDtoMapper;

    @GetMapping("/analytics")
    @Operation(summary = "KYC queue analytics (admin)")
    public ResponseEntity<ApiResponse<KycAnalyticsResult>> analytics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                getKycAnalyticsQueryPort.execute(from, to), "KYC analytics fetched"));
    }

    @GetMapping
    @Operation(summary = "List KYC profiles by status (admin)",
            description = "List KYC profiles in a given status, ordered by submitted date desc")
    public ResponseEntity<ApiResponse<List<KycResponse>>> listByStatus(
            @RequestParam(defaultValue = "SUBMITTED") KycStatus status,
            @RequestParam(defaultValue = "50") int limit) {
        var result = listAdminKycQueryPort.execute(status, limit);
        return ResponseEntity.ok(ApiResponse.success(
                kycDtoMapper.toResponses(result), "KYC profiles fetched"));
    }

    @GetMapping("/{kycId}")
    @Operation(summary = "Get KYC profile (admin)")
    public ResponseEntity<ApiResponse<KycResponse>> get(@PathVariable String kycId) {
        var result = getAdminKycQueryPort.execute(kycId);
        return ResponseEntity.ok(ApiResponse.success(
                kycDtoMapper.toResponse(result), "KYC profile fetched"));
    }

    @PostMapping("/{kycId}/in-review")
    @Operation(summary = "Mark KYC as IN_REVIEW (admin)",
            description = "Move a SUBMITTED profile into IN_REVIEW state and assign current admin as reviewer")
    public ResponseEntity<ApiResponse<KycResponse>> markInReview(
            Authentication authentication,
            @PathVariable String kycId,
            @Valid @RequestBody(required = false) AdminMarkInReviewKycRequest request) {
        String note = request != null ? request.note() : null;
        var result = markKycInReviewInputPort.execute(new KycAdminCommands.MarkInReviewKyc(
                kycId, authentication.getName(), note));
        return ResponseEntity.ok(ApiResponse.success(
                kycDtoMapper.toResponse(result), "KYC marked in review"));
    }

    @PostMapping("/{kycId}/approve")
    @Operation(summary = "Approve KYC (admin)")
    public ResponseEntity<ApiResponse<KycResponse>> approve(
            Authentication authentication,
            @PathVariable String kycId,
            @Valid @RequestBody(required = false) AdminApproveKycRequest request) {
        String note = request != null ? request.note() : null;
        var result = approveKycInputPort.execute(new KycAdminCommands.ApproveKyc(
                kycId, authentication.getName(), note));
        return ResponseEntity.ok(ApiResponse.success(
                kycDtoMapper.toResponse(result), "KYC approved"));
    }

    @PostMapping("/{kycId}/reject")
    @Operation(summary = "Reject KYC (admin)")
    public ResponseEntity<ApiResponse<KycResponse>> reject(
            Authentication authentication,
            @PathVariable String kycId,
            @Valid @RequestBody AdminRejectKycRequest request) {
        var result = rejectKycInputPort.execute(new KycAdminCommands.RejectKyc(
                kycId, authentication.getName(), request.reason()));
        return ResponseEntity.ok(ApiResponse.success(
                kycDtoMapper.toResponse(result), "KYC rejected"));
    }
}
