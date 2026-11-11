package com.aionn.catalog.infrastructure.persistence.adapter.merchant;

import com.aionn.catalog.application.port.out.merchant.WishlistPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class WishlistJdbcAdapter implements WishlistPersistencePort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void add(String userId, String productId, Instant addedAt) {
        jdbcTemplate.update("""
                INSERT INTO wishlist_items (user_id, product_id, added_at)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id, product_id) DO NOTHING
                """, userId, productId, Timestamp.from(addedAt));
    }

    @Override
    public void remove(String userId, String productId) {
        jdbcTemplate.update(
                "DELETE FROM wishlist_items WHERE user_id = ? AND product_id = ?",
                userId, productId);
    }

    @Override
    public boolean exists(String userId, String productId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM wishlist_items WHERE user_id = ? AND product_id = ?
                """, Integer.class, userId, productId);
        return count != null && count > 0;
    }

    @Override
    public List<String> findProductIds(String userId, int offset, int limit) {
        return jdbcTemplate.queryForList("""
                SELECT product_id FROM wishlist_items
                 WHERE user_id = ?
                 ORDER BY added_at DESC
                 LIMIT ? OFFSET ?
                """, String.class, userId, limit, offset);
    }

    @Override
    public long count(String userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wishlist_items WHERE user_id = ?",
                Long.class, userId);
        return count == null ? 0L : count;
    }
}
