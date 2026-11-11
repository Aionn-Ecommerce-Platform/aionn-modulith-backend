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
        int inserted = jdbc.update("""
                INSERT INTO order_placement_operations
                    (user_id, idempotency_key, request_hash, order_id)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id, idempotency_key) DO NOTHING
                """, userId, key, requestHash, proposedOrderId);
        List<java.util.Map<String, Object>> rows = jdbc.queryForList("""
                SELECT order_id, request_hash, status FROM order_placement_operations
                 WHERE user_id = ? AND idempotency_key = ?
                """, userId, key);
        var row = rows.getFirst();
        String status = (String) row.get("status");
        boolean completed = "COMPLETED".equals(status);
        boolean claimed = inserted > 0;
        if (!claimed && !completed) {
            int takeover = jdbc.update("""
                    UPDATE order_placement_operations SET updated_at = NOW()
                     WHERE user_id = ? AND idempotency_key = ? AND status = 'PENDING'
                       AND updated_at < NOW() - INTERVAL '2 minutes'
                    """, userId, key);
            claimed = takeover == 1;
        }
        Operation operation = new Operation(
                (String) row.get("order_id"), (String) row.get("request_hash"), completed, claimed);
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
