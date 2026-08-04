package com.aionn.config;

import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.infrastructure.outbox.OutboxDeadLetterService;
import com.aionn.sharedkernel.infrastructure.outbox.OutboxDeadLetterService.DeadLetterPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/outbox/dead-letters")
@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
@Tag(name = "Operations - Outbox", description = "Inspect and recover transactional outbox dead letters")
public class OutboxOperationsController {

    private final OutboxDeadLetterService deadLetterService;

    @GetMapping
    @Operation(summary = "List dead-letter events")
    public ResponseEntity<ApiResponse<DeadLetterPage>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success(
                deadLetterService.list(page, size), "Dead-letter events retrieved"));
    }

    @PostMapping("/{eventId}/requeue")
    @Operation(summary = "Requeue a dead-letter event")
    public ResponseEntity<ApiResponse<Void>> requeue(@PathVariable String eventId) {
        deadLetterService.requeue(eventId);
        return ResponseEntity.ok(ApiResponse.success("Dead-letter event requeued"));
    }
}
