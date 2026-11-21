package com.aionn.ordering.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface OrderReturnItemPersistencePort {

    void saveAll(String returnId, List<Item> items);

    List<Item> findByReturnId(String returnId);

    Map<String, Integer> returnedQuantities(String orderId);

    record Item(
            String skuId,
            int qty,
            BigDecimal unitPrice,
            BigDecimal refundAmount) {
    }
}
