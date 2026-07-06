package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.category.CreateCategoryRequest;
import com.aionn.catalog.adapter.rest.dto.category.MoveCategoryRequest;
import com.aionn.catalog.adapter.rest.dto.category.UpdateCategoryRequest;
import com.aionn.catalog.application.dto.category.command.DeleteCategoryCommand;
import com.aionn.catalog.application.dto.category.query.GetCategoryQuery;
import com.aionn.catalog.application.dto.category.query.ListCategoryChildrenQuery;
import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.application.dto.category.result.CategoryTreeNode;
import com.aionn.catalog.application.port.in.category.CreateCategoryInputPort;
import com.aionn.catalog.application.port.in.category.DeleteCategoryInputPort;
import com.aionn.catalog.application.port.in.category.GetCategoryInputPort;
import com.aionn.catalog.application.port.in.category.GetCategoryTreeInputPort;
import com.aionn.catalog.application.port.in.category.ListCategoryChildrenInputPort;
import com.aionn.catalog.application.port.in.category.ListCategoryRootsInputPort;
import com.aionn.catalog.application.port.in.category.MoveCategoryInputPort;
import com.aionn.catalog.application.port.in.category.UpdateCategoryInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.aionn.catalog.adapter.rest.mapper.category.CategoryDtoMapper;

@RestController
@RequestMapping("/api/v1/catalog/categories")
@RequiredArgsConstructor
@Tag(name = "Catalog - Category", description = "Category tree management")
public class CategoryController {

    private final CreateCategoryInputPort createCategoryInputPort;
    private final UpdateCategoryInputPort updateCategoryInputPort;
    private final MoveCategoryInputPort moveCategoryInputPort;
    private final DeleteCategoryInputPort deleteCategoryInputPort;
    private final ListCategoryRootsInputPort listCategoryRootsInputPort;
    private final ListCategoryChildrenInputPort listCategoryChildrenInputPort;
    private final GetCategoryTreeInputPort getCategoryTreeInputPort;
    private final GetCategoryInputPort getCategoryInputPort;
    private final CategoryDtoMapper categoryDtoMapper;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Create category")
    public ResponseEntity<ApiResponse<CategoryResult>> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResult result = createCategoryInputPort.execute(
                categoryDtoMapper.toCreateCategoryCommand(request));
        return ApiResponse.createdResponse("Category created", result);
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Update category", description = "Edit category content (name, icon, active flag). CS agents may correct content; structural changes (create/move/delete) remain SYSTEM_ADMIN only.")
    public ResponseEntity<ApiResponse<CategoryResult>> update(
            @PathVariable String categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        CategoryResult result = updateCategoryInputPort.execute(
                categoryDtoMapper.toUpdateCategoryCommand(categoryId, request));
        return ResponseEntity.ok(ApiResponse.success(result, "Category updated"));
    }

    @PostMapping("/{categoryId}/move")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Move category", description = "Reparent the category in the tree")
    public ResponseEntity<ApiResponse<CategoryResult>> move(
            @PathVariable String categoryId,
            @Valid @RequestBody MoveCategoryRequest request) {
        CategoryResult result = moveCategoryInputPort
                .execute(categoryDtoMapper.toMoveCategoryCommand(categoryId, request));
        return ResponseEntity.ok(ApiResponse.success(result, "Category moved"));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Delete category", description = "Soft delete only")
    public ResponseEntity<Void> delete(@PathVariable String categoryId) {
        deleteCategoryInputPort.execute(new DeleteCategoryCommand(categoryId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roots")
    @Operation(summary = "List root categories", description = "Public read - active root categories")
    public ResponseEntity<ApiResponse<List<CategoryResult>>> listRoots() {
        return ResponseEntity.ok(ApiResponse.success(
                listCategoryRootsInputPort.execute(), "Root categories fetched"));
    }

    @GetMapping("/{categoryId}/children")
    @Operation(summary = "List children of a category", description = "Public read")
    public ResponseEntity<ApiResponse<List<CategoryResult>>> listChildren(@PathVariable String categoryId) {
        return ResponseEntity.ok(ApiResponse.success(
                listCategoryChildrenInputPort.execute(new ListCategoryChildrenQuery(categoryId)), "Children fetched"));
    }

    @GetMapping("/tree")
    @Operation(summary = "Full category tree", description = "Public read - nested tree of all active categories")
    public ResponseEntity<ApiResponse<List<CategoryTreeNode>>> tree() {
        return ResponseEntity.ok(ApiResponse.success(
                getCategoryTreeInputPort.execute(), "Category tree fetched"));
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get category", description = "Public read")
    public ResponseEntity<ApiResponse<CategoryResult>> get(@PathVariable String categoryId) {
        return ResponseEntity.ok(ApiResponse.success(
                getCategoryInputPort.execute(new GetCategoryQuery(categoryId)), "Category fetched"));
    }
}
