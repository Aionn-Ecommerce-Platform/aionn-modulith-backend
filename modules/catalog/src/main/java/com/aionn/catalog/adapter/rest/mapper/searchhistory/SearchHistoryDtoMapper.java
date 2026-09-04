package com.aionn.catalog.adapter.rest.mapper.searchhistory;

import com.aionn.catalog.adapter.rest.dto.searchhistory.request.RecordSearchesRequest;
import com.aionn.catalog.adapter.rest.dto.searchhistory.response.SearchHistoryResponse;
import com.aionn.catalog.application.dto.searchhistory.command.RecordSearchesCommand;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SearchHistoryDtoMapper {

    default RecordSearchesCommand toCommand(String userId, RecordSearchesRequest request) {
        return new RecordSearchesCommand(userId, List.copyOf(request.queries()));
    }

    default SearchHistoryResponse toResponse(List<String> queries) {
        return new SearchHistoryResponse(List.copyOf(queries));
    }
}
