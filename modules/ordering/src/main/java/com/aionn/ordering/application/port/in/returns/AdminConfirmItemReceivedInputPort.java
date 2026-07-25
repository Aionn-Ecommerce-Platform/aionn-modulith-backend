package com.aionn.ordering.application.port.in.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnResult;

public interface AdminConfirmItemReceivedInputPort {
    ReturnResult execute(String returnId, String itemCondition);
}
