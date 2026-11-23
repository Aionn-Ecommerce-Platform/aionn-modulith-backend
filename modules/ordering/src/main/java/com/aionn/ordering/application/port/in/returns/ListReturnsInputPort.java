package com.aionn.ordering.application.port.in.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnPageResult;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;

import java.util.List;

public interface ListReturnsInputPort {
    List<ReturnResult> execute(String requesterId, String type, int limit);

    ReturnPageResult executePaged(
            String requesterId,
            String type,
            String status,
            String reasonType,
            String search,
            int page,
            int limit);
}
