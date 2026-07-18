package com.aionn.ordering.application.port.in.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import java.util.List;

public interface ListReturnsInputPort {
    List<ReturnResult> execute(String requesterId, String type, int limit);
}