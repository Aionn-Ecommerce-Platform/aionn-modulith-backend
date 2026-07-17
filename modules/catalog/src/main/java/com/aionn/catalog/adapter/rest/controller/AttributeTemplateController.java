package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.attribute.request.ConfigureFilterableRequest;
import com.aionn.catalog.adapter.rest.dto.attribute.request.CreateAttributeTemplateRequest;
import com.aionn.catalog.adapter.rest.dto.attribute.response.AttributeTemplateResponse;
import com.aionn.catalog.application.dto.attribute.query.GetAttributeTemplateByCategoryQuery;
import com.aionn.catalog.application.dto.attribute.query.GetAttributeTemplateQuery;
import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;
import com.aionn.catalog.application.port.in.attribute.ConfigureFilterableInputPort;
import com.aionn.catalog.application.port.in.attribute.CreateAttributeTemplateInputPort;
import com.aionn.catalog.application.port.in.attribute.GetAttributeTemplateByCategoryInputPort;
import com.aionn.catalog.application.port.in.attribute.GetAttributeTemplateInputPort;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import com.aionn.catalog.adapter.rest.mapper.attribute.AttributeDtoMapper;

@RestController
@RequestMapping("/api/v1/catalog/attribute-templates")
@RequiredArgsConstructor
@Tag(name = "Catalog - AttributeTemplate", description = "Per-category attribute templates")
public class AttributeTemplateController {

        private final CreateAttributeTemplateInputPort createAttributeTemplateInputPort;
        private final ConfigureFilterableInputPort configureFilterableInputPort;
        private final GetAttributeTemplateInputPort getAttributeTemplateInputPort;
        private final AttributeDtoMapper attributeDtoMapper;
        private final GetAttributeTemplateByCategoryInputPort getAttributeTemplateByCategoryInputPort;

        @PostMapping
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Create attribute template")
        public ResponseEntity<ApiResponse<AttributeTemplateResponse>> create(
                        @Valid @RequestBody CreateAttributeTemplateRequest request) {
                AttributeTemplateResult result = createAttributeTemplateInputPort.execute(
                                attributeDtoMapper.toCreateAttributeTemplateCommand(request));
                return ApiResponse.createdResponse("Attribute template created", attributeDtoMapper.toResponse(result));
        }

        @PutMapping("/{templateId}/filterable")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Configure filterable", description = "Mark a key as filterable for the AI agent")
        public ResponseEntity<ApiResponse<AttributeTemplateResponse>> configureFilterable(
                        @PathVariable String templateId,
                        @Valid @RequestBody ConfigureFilterableRequest request) {
                AttributeTemplateResult result = configureFilterableInputPort.execute(
                                attributeDtoMapper.toConfigureFilterableCommand(templateId, request));
                return ResponseEntity.ok(ApiResponse.success(attributeDtoMapper.toResponse(result), "Filterable updated"));
        }

        @GetMapping("/{templateId}")
        @Operation(summary = "Get attribute template", description = "Public read")
        public ResponseEntity<ApiResponse<AttributeTemplateResponse>> get(@PathVariable String templateId) {
                return ResponseEntity.ok(ApiResponse.success(
                                attributeDtoMapper.toResponse(getAttributeTemplateInputPort.execute(new GetAttributeTemplateQuery(templateId))),
                                "Attribute template fetched"));
        }

        @GetMapping
        @Operation(summary = "Get attribute template by category", description = "Public read - used by product creation UI to render the attribute form")
        public ResponseEntity<ApiResponse<AttributeTemplateResponse>> getByCategory(
                        @RequestParam String categoryId) {
                return ResponseEntity.ok(ApiResponse.success(
                                attributeDtoMapper.toResponse(getAttributeTemplateByCategoryInputPort
                                                 .execute(new GetAttributeTemplateByCategoryQuery(categoryId))),
                                "Attribute template fetched"));
        }
}
