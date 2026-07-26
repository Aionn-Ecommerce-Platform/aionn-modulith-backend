package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.dto.template.CreateTemplateRequest;
import com.aionn.notification.adapter.rest.dto.template.UpdateTemplateRequest;
import com.aionn.notification.application.dto.template.command.TemplateCommands;
import com.aionn.notification.application.dto.template.result.TemplateResult;
import com.aionn.notification.application.service.NotificationTemplateService;
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
@RequestMapping("/api/v1/notifications/templates")
@RequiredArgsConstructor
@Tag(name = "Notification - Template", description = "Template management")
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Create template", description = "UC8.6")
    public ResponseEntity<ApiResponse<TemplateResult>> create(@Valid @RequestBody CreateTemplateRequest request) {
        return ApiResponse.createdResponse("Template created",
                templateService.create(new TemplateCommands.CreateTemplate(
                        request.eventType(), request.channel(), request.category(),
                        request.locale(), request.subject(), request.content())));
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Update template", description = "UC8.7")
    public ResponseEntity<ApiResponse<TemplateResult>> update(
            @PathVariable String templateId,
            @Valid @RequestBody UpdateTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                templateService.update(new TemplateCommands.UpdateTemplate(
                        templateId, request.subject(), request.content())),
                "Template updated"));
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Get template")
    public ResponseEntity<ApiResponse<TemplateResult>> get(@PathVariable String templateId) {
        return ResponseEntity.ok(ApiResponse.success(templateService.get(templateId), "Template fetched"));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "List templates")
    public ResponseEntity<ApiResponse<List<TemplateResult>>> list(
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(ApiResponse.success(templateService.list(limit), "Templates fetched"));
    }
}
