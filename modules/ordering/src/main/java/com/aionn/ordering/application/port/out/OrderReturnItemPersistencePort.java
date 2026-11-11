package com.aionn.ordering.application.port.out;

import java.math.BigDecimal;
import java.util.List;

/** Line-level return selection for partial refunds. */
public interface OrderReturnItemPersistencePort {

    void saveAll(String returnId, List<Item> items);

    List<Item> findByReturnId(String returnId);

    record Item(
            String skuId,
            int qty,
            BigDecimal unitPrice,
            BigDecimal refundAmount) {
    }
}
