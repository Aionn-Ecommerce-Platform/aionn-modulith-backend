package com.aionn.catalog.application.port.in.searchhistory;

import java.util.List;

public interface GetRecentSearchesInputPort {

    List<String> execute(String userId);
}
