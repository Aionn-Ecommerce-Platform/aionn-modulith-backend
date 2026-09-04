package com.aionn.catalog.application.dto.searchhistory.command;

import java.util.List;

public record RecordSearchesCommand(String userId, List<String> queries) {
}
