package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.dto.template.CreateTemplateRequest;
import com.aionn.notification.adapter.rest.dto.template.UpdateTemplateRequest;
import com.aionn.notification.adapter.rest.dto.template.response.TemplateResponse;
import com.aionn.notification.adapter.rest.mapper.template.NotificationTemplateDtoMapper;
import com.aionn.notification.application.port.in.template.CreateTemplateInputPort;
import com.aionn.notification.application.port.in.template.GetTemplateInputPort;
import com.aionn.notification.application.port.in.template.ListTemplatesInputPort;
import com.aionn.notification.application.port.in.template.UpdateTemplateInputPort;
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

    private final CreateTemplateInputPort createTemplateInputPort;
    private final UpdateTemplateInputPort updateTemplateInputPort;
    private final GetTemplateInputPort getTemplateInputPort;
    private final ListTemplatesInputPort listTemplatesInputPort;
    private final NotificationTemplateDtoMapper dtoMapper;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Create template")
    public ResponseEntity<ApiResponse<TemplateResponse>> create(
            @Valid @RequestBody CreateTemplateRequest request) {
        return ApiResponse.createdResponse("Template created",
                dtoMapper.toResponse(createTemplateInputPort.execute(
                        dtoMapper.toCreateCommand(request))));
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Update template")
    public ResponseEntity<ApiResponse<TemplateResponse>> update(
            @PathVariable String templateId,
            @Valid @RequestBody UpdateTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                dtoMapper.toResponse(updateTemplateInputPort.execute(
                        dtoMapper.toUpdateCommand(templateId, request))),
                "Template updated"));
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Get template")
    public ResponseEntity<ApiResponse<TemplateResponse>> get(@PathVariable String templateId) {
        return ResponseEntity.ok(ApiResponse.success(
                dtoMapper.toResponse(getTemplateInputPort.execute(templateId)),
                "Template fetched"));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "List templates")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> list(
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                dtoMapper.toResponses(listTemplatesInputPort.execute(limit)),
                "Templates fetched"));
    }
}
