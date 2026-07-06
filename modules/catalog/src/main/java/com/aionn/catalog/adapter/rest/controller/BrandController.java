package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.brand.CreateBrandRequest;
import com.aionn.catalog.adapter.rest.dto.brand.DeleteBrandRequest;
import com.aionn.catalog.adapter.rest.dto.brand.UpdateBrandRequest;
import com.aionn.catalog.application.dto.brand.query.GetBrandQuery;
import com.aionn.catalog.application.dto.brand.query.ListBrandsQuery;
import com.aionn.catalog.application.dto.brand.result.BrandResult;
import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.port.in.brand.CreateBrandInputPort;
import com.aionn.catalog.application.port.in.brand.DeleteBrandInputPort;
import com.aionn.catalog.application.port.in.brand.GetBrandInputPort;
import com.aionn.catalog.application.port.in.brand.ListBrandsInputPort;
import com.aionn.catalog.application.port.in.brand.UpdateBrandInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.aionn.catalog.adapter.rest.mapper.brand.BrandDtoMapper;
import com.aionn.sharedkernel.adapter.web.response.PageMetadata;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/brands")
@RequiredArgsConstructor
@Tag(name = "Catalog - Brand", description = "Brand management")
public class BrandController {

    private final CreateBrandInputPort createBrandInputPort;
    private final UpdateBrandInputPort updateBrandInputPort;
    private final DeleteBrandInputPort deleteBrandInputPort;
    private final ListBrandsInputPort listBrandsInputPort;
    private final GetBrandInputPort getBrandInputPort;
    private final BrandDtoMapper brandDtoMapper;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Create brand")
    public ResponseEntity<ApiResponse<BrandResult>> create(@Valid @RequestBody CreateBrandRequest request) {
        BrandResult result = createBrandInputPort.execute(brandDtoMapper.toCreateBrandCommand(request));
        return ApiResponse.createdResponse("Brand created", result);
    }

    @PutMapping("/{brandId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Update brand", description = "Edit brand content (name, logo, description). CS agents may correct content; structural changes (create/delete) remain SYSTEM_ADMIN only.")
    public ResponseEntity<ApiResponse<BrandResult>> update(
            @PathVariable String brandId,
            @Valid @RequestBody UpdateBrandRequest request) {
        BrandResult result = updateBrandInputPort.execute(brandDtoMapper.toUpdateBrandCommand(brandId, request));
        return ResponseEntity.ok(ApiResponse.success(result, "Brand updated"));
    }

    @PostMapping("/{brandId}/delete")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Delete brand", description = "Soft delete only")
    public ResponseEntity<Void> delete(
            @PathVariable String brandId,
            @Valid @RequestBody DeleteBrandRequest request) {
        deleteBrandInputPort.execute(brandDtoMapper.toDeleteBrandCommand(brandId, request));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List brands", description = "Public paginated list of brands")
    public ResponseEntity<ApiResponse<List<BrandResult>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<BrandResult> result = listBrandsInputPort.execute(
                new ListBrandsQuery(OffsetPagination.of(page, size)));
        return ResponseEntity.ok(ApiResponse.successWithPaging(
                result.content(),
                PageMetadata.of(result.page(), result.size(), result.totalElements()),
                "Brands fetched"));
    }

    @GetMapping("/{brandId}")
    @Operation(summary = "Get brand", description = "Public read")
    public ResponseEntity<ApiResponse<BrandResult>> get(@PathVariable String brandId) {
        return ResponseEntity.ok(ApiResponse.success(
                getBrandInputPort.execute(new GetBrandQuery(brandId)), "Brand fetched"));
    }
}
