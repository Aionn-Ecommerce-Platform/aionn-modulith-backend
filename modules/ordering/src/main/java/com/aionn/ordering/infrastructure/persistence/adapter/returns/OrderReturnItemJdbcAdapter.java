package com.aionn.ordering.infrastructure.persistence.adapter.returns;

import com.aionn.ordering.application.port.out.OrderReturnItemPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderReturnItemJdbcAdapter implements OrderReturnItemPersistencePort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void saveAll(String returnId, List<Item> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO order_return_items (return_id, sku_id, qty, unit_price, refund_amount)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (return_id, sku_id) DO UPDATE
                   SET qty = EXCLUDED.qty,
                       unit_price = EXCLUDED.unit_price,
                       refund_amount = EXCLUDED.refund_amount
                """, items.stream()
                .map(item -> new Object[] {
                        returnId, item.skuId(), item.qty(), item.unitPrice(), item.refundAmount() })
                .toList());
    }

    @Override
    public List<Item> findByReturnId(String returnId) {
        return jdbcTemplate.query("""
                SELECT sku_id, qty, unit_price, refund_amount
                  FROM order_return_items
                 WHERE return_id = ?
                 ORDER BY sku_id
                """, (rs, rowNum) -> new Item(
                        rs.getString("sku_id"),
                        rs.getInt("qty"),
                        rs.getBigDecimal("unit_price"),
                        rs.getBigDecimal("refund_amount")), returnId);
    }
}
