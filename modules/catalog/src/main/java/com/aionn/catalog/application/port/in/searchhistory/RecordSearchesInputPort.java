package com.aionn.catalog.application.port.in.searchhistory;

import com.aionn.catalog.application.dto.searchhistory.command.RecordSearchesCommand;

import java.util.List;

public interface RecordSearchesInputPort {

    List<String> execute(RecordSearchesCommand command);
}
