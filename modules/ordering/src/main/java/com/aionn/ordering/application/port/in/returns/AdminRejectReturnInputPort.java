package com.aionn.ordering.application.port.in.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnResult;

public interface AdminRejectReturnInputPort {
    ReturnResult execute(String returnId, String reason);
}
