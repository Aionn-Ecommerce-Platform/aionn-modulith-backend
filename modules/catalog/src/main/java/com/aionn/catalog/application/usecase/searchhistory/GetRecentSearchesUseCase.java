package com.aionn.catalog.application.usecase.searchhistory;

import com.aionn.catalog.application.port.in.searchhistory.GetRecentSearchesInputPort;
import com.aionn.catalog.application.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetRecentSearchesUseCase implements GetRecentSearchesInputPort {

    private final SearchHistoryService searchHistoryService;

    @Override
    public List<String> execute(String userId) {
        return searchHistoryService.getRecentSearches(userId);
    }
}
