package com.aionn.catalog.infrastructure.persistence.adapter.merchant;

import com.aionn.catalog.application.port.out.merchant.StockAlertPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockAlertJdbcAdapter implements StockAlertPersistencePort {

    private static final RowMapper<PendingAlert> MAPPER = (rs, rowNum) -> new PendingAlert(
            rs.getString("alert_id"),
            rs.getString("user_id"),
            rs.getString("product_id"),
            rs.getString("sku_id"),
            rs.getTimestamp("created_at").toInstant());

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void subscribe(String alertId, String userId, String productId, String skuId, Instant createdAt) {
        jdbcTemplate.update("""
                INSERT INTO stock_alerts (alert_id, user_id, product_id, sku_id, created_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (user_id, sku_id) DO NOTHING
                """, alertId, userId, productId, skuId, Timestamp.from(createdAt));
    }

    @Override
    public void unsubscribe(String userId, String skuId) {
        jdbcTemplate.update(
                "DELETE FROM stock_alerts WHERE user_id = ? AND sku_id = ?",
                userId, skuId);
    }

    @Override
    public boolean exists(String userId, String skuId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM stock_alerts WHERE user_id = ? AND sku_id = ?
                """, Integer.class, userId, skuId);
        return count != null && count > 0;
    }

    @Override
    public List<PendingAlert> findPendingBySku(String skuId) {
        return jdbcTemplate.query("""
                SELECT alert_id, user_id, product_id, sku_id, created_at
                  FROM stock_alerts
                 WHERE sku_id = ? AND notified_at IS NULL
                """, MAPPER, skuId);
    }

    @Override
    public void markNotified(String alertId, Instant notifiedAt) {
        jdbcTemplate.update("""
                UPDATE stock_alerts SET notified_at = ? WHERE alert_id = ? AND notified_at IS NULL
                """, Timestamp.from(notifiedAt), alertId);
    }
}
