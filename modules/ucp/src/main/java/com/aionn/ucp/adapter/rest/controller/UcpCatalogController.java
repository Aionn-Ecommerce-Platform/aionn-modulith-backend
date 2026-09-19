package com.aionn.ucp.adapter.rest.controller;

import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogLookupRequest;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogLookupResponse;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogSearchRequest;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogSearchResponse;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpProductDetailRequest;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpProductDetailResponse;
import com.aionn.ucp.application.catalog.UcpCatalogApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing canonical UCP Catalog REST endpoints.
 * Provides search, batch lookup, and single-product retrieval without leaking
 * internal catalog domain types.
 */
@RestController
@RequestMapping("/ucp/v1/catalog")
@RequiredArgsConstructor
public class UcpCatalogController {

    private final UcpCatalogApplicationService catalogService;

    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UcpCatalogSearchResponse> search(
            @Valid @RequestBody(required = false) UcpCatalogSearchRequest request) {
        UcpCatalogSearchResponse response = catalogService.searchCatalog(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/lookup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UcpCatalogLookupResponse> lookup(
            @Valid @RequestBody UcpCatalogLookupRequest request) {
        UcpCatalogLookupResponse response = catalogService.lookupCatalog(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/product", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UcpProductDetailResponse> getProduct(
            @Valid @RequestBody UcpProductDetailRequest request) {
        UcpProductDetailResponse response = catalogService.getProduct(request);
        return ResponseEntity.ok(response);
    }
}
