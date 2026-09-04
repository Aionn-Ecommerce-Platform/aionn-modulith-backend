package com.aionn.catalog.application.usecase.searchhistory;

import com.aionn.catalog.application.dto.searchhistory.command.RecordSearchesCommand;
import com.aionn.catalog.application.port.in.searchhistory.RecordSearchesInputPort;
import com.aionn.catalog.application.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordSearchesUseCase implements RecordSearchesInputPort {

    private final SearchHistoryService searchHistoryService;

    @Override
    public List<String> execute(RecordSearchesCommand command) {
        return searchHistoryService.recordSearches(command.userId(), command.queries());
    }
}
