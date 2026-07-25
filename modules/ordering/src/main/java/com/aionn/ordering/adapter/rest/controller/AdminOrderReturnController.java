package com.aionn.ordering.adapter.rest.controller;

import com.aionn.ordering.adapter.rest.dto.request.AdminApproveReturnRequest;
import com.aionn.ordering.adapter.rest.dto.request.AdminConfirmReturnReceivedRequest;
import com.aionn.ordering.adapter.rest.dto.request.AdminRejectReturnRequest;
import com.aionn.ordering.adapter.rest.dto.response.OrderReturnResponse;
import com.aionn.ordering.adapter.rest.mapper.OrderingDtoMapper;
import com.aionn.ordering.application.dto.returns.result.ReturnAnalyticsResult;
import com.aionn.ordering.application.port.in.returns.AdminApproveReturnInputPort;
import com.aionn.ordering.application.port.in.returns.AdminConfirmItemReceivedInputPort;
import com.aionn.ordering.application.port.in.returns.AdminGetReturnInputPort;
import com.aionn.ordering.application.port.in.returns.AdminListReturnsInputPort;
import com.aionn.ordering.application.port.in.returns.AdminRejectReturnInputPort;
import com.aionn.ordering.application.port.in.returns.AdminReturnAnalyticsInputPort;
import com.aionn.ordering.domain.valueobject.ReturnStatus;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/admin/ordering/returns")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
@Tag(name = "Ordering - Return Admin", description = "Admin override for buyer-merchant return disputes (force approve/reject/receive)")
public class AdminOrderReturnController {

        private final AdminListReturnsInputPort adminListReturnsInputPort;
        private final AdminGetReturnInputPort adminGetReturnInputPort;
        private final AdminReturnAnalyticsInputPort adminReturnAnalyticsInputPort;
        private final AdminApproveReturnInputPort adminApproveReturnInputPort;
        private final AdminRejectReturnInputPort adminRejectReturnInputPort;
        private final AdminConfirmItemReceivedInputPort adminConfirmItemReceivedInputPort;
        private final OrderingDtoMapper dtoMapper;

        @GetMapping
        @Operation(summary = "List returns by status (admin)")
        public ResponseEntity<ApiResponse<List<OrderReturnResponse>>> listByStatus(
                        @RequestParam(defaultValue = "REQUESTED") ReturnStatus status,
                        @RequestParam(defaultValue = "50") int limit) {
                List<OrderReturnResponse> responses = adminListReturnsInputPort.execute(status, limit).stream()
                                .map(dtoMapper::toResponse)
                                .toList();
                return ResponseEntity.ok(ApiResponse.success(responses, "Returns fetched"));
        }

        @GetMapping("/analytics")
        @Operation(summary = "Return analytics (admin)")
        public ResponseEntity<ApiResponse<ReturnAnalyticsResult>> analytics(
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
                return ResponseEntity.ok(ApiResponse.success(
                                adminReturnAnalyticsInputPort.execute(from, to), "Return analytics fetched"));
        }

        @GetMapping("/{returnId}")
        @Operation(summary = "Get return (admin)")
        public ResponseEntity<ApiResponse<OrderReturnResponse>> get(@PathVariable String returnId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(adminGetReturnInputPort.execute(returnId)),
                                "Return fetched"));
        }

        @PostMapping("/{returnId}/approve")
        @Operation(summary = "Force-approve return (admin)", description = "Admin override when merchant fails to act on a buyer-initiated return")
        public ResponseEntity<ApiResponse<OrderReturnResponse>> approve(
                        @PathVariable String returnId,
                        @Valid @RequestBody AdminApproveReturnRequest request) {
                OrderReturnResponse response = dtoMapper.toResponse(adminApproveReturnInputPort.execute(
                                returnId, request.refundAmount(), request.currency(),
                                request.returnWarehouseId()));
                return ResponseEntity.ok(ApiResponse.success(response, "Return approved by admin"));
        }

        @PostMapping("/{returnId}/reject")
        @Operation(summary = "Force-reject return (admin)")
        public ResponseEntity<ApiResponse<OrderReturnResponse>> reject(
                        @PathVariable String returnId,
                        @Valid @RequestBody AdminRejectReturnRequest request) {
                OrderReturnResponse response = dtoMapper.toResponse(
                                adminRejectReturnInputPort.execute(returnId, request.reason()));
                return ResponseEntity.ok(ApiResponse.success(response, "Return rejected by admin"));
        }

        @PostMapping("/{returnId}/item-received")
        @Operation(summary = "Force-confirm return item received (admin)")
        public ResponseEntity<ApiResponse<OrderReturnResponse>> confirmReceived(
                        @PathVariable String returnId,
                        @Valid @RequestBody AdminConfirmReturnReceivedRequest request) {
                OrderReturnResponse response = dtoMapper.toResponse(adminConfirmItemReceivedInputPort.execute(
                                returnId, request.itemCondition()));
                return ResponseEntity.ok(ApiResponse.success(response, "Return item received recorded by admin"));
        }
}
