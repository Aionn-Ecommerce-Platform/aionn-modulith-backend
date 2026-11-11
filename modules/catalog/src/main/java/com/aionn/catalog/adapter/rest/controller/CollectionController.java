package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.application.port.out.merchant.CollectionPersistencePort.CollectionView;
import com.aionn.catalog.application.service.CollectionService;
import com.aionn.platform.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/collections")
@RequiredArgsConstructor
@Tag(name = "Catalog - Collections", description = "Curated product collections")
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    @Operation(summary = "List collections")
    public ResponseEntity<ApiResponse<List<CollectionResponse>>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(
                collectionService.list(activeOnly).stream().map(CollectionController::toResponse).toList(),
                "Collections fetched"));
    }

    @GetMapping("/{key}")
    @Operation(summary = "Get a collection by id or slug")
    public ResponseEntity<ApiResponse<CollectionResponse>> get(@PathVariable String key) {
        return ResponseEntity.ok(ApiResponse.success(
                toResponse(collectionService.get(key)), "Collection fetched"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Create a collection")
    public ResponseEntity<ApiResponse<CollectionResponse>> create(
            @Valid @RequestBody SaveCollectionRequest request) {
        CollectionView created = collectionService.create(
                request.slug(), request.name(), request.description(), request.imageUrl(),
                request.active() == null || request.active(), request.displayOrder() == null ? 0 : request.displayOrder());
        return ResponseEntity.ok(ApiResponse.success(
                toResponse(created), "Collection created"));
    }

    @PutMapping("/{collectionId}")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Update a collection")
    public ResponseEntity<ApiResponse<CollectionResponse>> update(
            @PathVariable String collectionId,
            @Valid @RequestBody SaveCollectionRequest request) {
        CollectionView updated = collectionService.update(
                collectionId, request.slug(), request.name(), request.description(), request.imageUrl(),
                request.active() == null || request.active(), request.displayOrder() == null ? 0 : request.displayOrder());
        return ResponseEntity.ok(ApiResponse.success(
                toResponse(updated), "Collection updated"));
    }

    @DeleteMapping("/{collectionId}")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Delete a collection")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String collectionId) {
        collectionService.delete(collectionId);
        return ResponseEntity.ok(ApiResponse.success("Collection deleted"));
    }

    private static CollectionResponse toResponse(CollectionView view) {
        return new CollectionResponse(
                view.collectionId(),
                view.slug(),
                view.name(),
                view.description(),
                view.imageUrl(),
                view.active(),
                view.displayOrder(),
                view.createdAt(),
                view.updatedAt());
    }

    public record SaveCollectionRequest(
            @Size(max = 100) String slug,
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description,
            @Size(max = 1000) String imageUrl,
            Boolean active,
            Integer displayOrder) {
    }

    public record CollectionResponse(
            String collectionId,
            String slug,
            String name,
            String description,
            String imageUrl,
            boolean active,
            int displayOrder,
            Instant createdAt,
            Instant updatedAt) {
    }
}
