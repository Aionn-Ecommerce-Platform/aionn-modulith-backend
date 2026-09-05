package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.searchhistory.request.RecordSearchesRequest;
import com.aionn.catalog.adapter.rest.dto.searchhistory.response.SearchHistoryResponse;
import com.aionn.catalog.adapter.rest.mapper.searchhistory.SearchHistoryDtoMapper;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerId;
import com.aionn.catalog.application.port.in.searchhistory.GetRecentSearchesInputPort;
import com.aionn.catalog.application.port.in.searchhistory.RecordSearchesInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/search-history")
@RequiredArgsConstructor
@Tag(name = "Catalog - Search History", description = "Authenticated user's recent catalog searches")
public class SearchHistoryController {

    private final GetRecentSearchesInputPort getRecentSearchesInputPort;
    private final RecordSearchesInputPort recordSearchesInputPort;
    private final SearchHistoryDtoMapper searchHistoryDtoMapper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get recent searches")
    public ResponseEntity<ApiResponse<SearchHistoryResponse>> getRecentSearches(
            @CurrentOwnerId String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                searchHistoryDtoMapper.toResponse(getRecentSearchesInputPort.execute(userId)),
                "Recent searches fetched"));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Record recent searches")
    public ResponseEntity<ApiResponse<SearchHistoryResponse>> recordSearches(
            @CurrentOwnerId String userId,
            @Valid @RequestBody RecordSearchesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                searchHistoryDtoMapper.toResponse(
                        recordSearchesInputPort.execute(searchHistoryDtoMapper.toCommand(userId, request))),
                "Recent searches recorded"));
    }
}
