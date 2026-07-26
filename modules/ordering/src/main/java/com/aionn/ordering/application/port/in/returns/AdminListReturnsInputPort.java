package com.aionn.ordering.application.port.in.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.domain.valueobject.ReturnStatus;

import java.util.List;

public interface AdminListReturnsInputPort {
    List<ReturnResult> execute(ReturnStatus status, int limit);
}
