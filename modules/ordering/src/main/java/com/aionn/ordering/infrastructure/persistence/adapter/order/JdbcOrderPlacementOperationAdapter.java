package com.aionn.ordering.infrastructure.persistence.adapter.order;

import com.aionn.ordering.application.port.out.OrderPlacementOperationPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JdbcOrderPlacementOperationAdapter implements OrderPlacementOperationPort {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public Operation start(String userId, String key, String requestHash, String proposedOrderId) {
        jdbc.update("""
                INSERT INTO order_placement_operations
                    (user_id, idempotency_key, request_hash, order_id)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id, idempotency_key) DO NOTHING
                """, userId, key, requestHash, proposedOrderId);
        List<Operation> operations = jdbc.query("""
                SELECT order_id, request_hash, status FROM order_placement_operations
                 WHERE user_id = ? AND idempotency_key = ?
                """, (rs, row) -> new Operation(rs.getString("order_id"), rs.getString("request_hash"),
                        "COMPLETED".equals(rs.getString("status"))), userId, key);
        Operation operation = operations.getFirst();
        if (!operation.requestHash().equals(requestHash)) {
            throw new IllegalArgumentException("Idempotency key was already used with a different order request");
        }
        return operation;
    }

    @Override
    public void complete(String userId, String key, String orderId) {
        jdbc.update("""
                UPDATE order_placement_operations SET status = 'COMPLETED', updated_at = NOW()
                 WHERE user_id = ? AND idempotency_key = ? AND order_id = ?
                """, userId, key, orderId);
    }
}
