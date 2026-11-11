package com.aionn.catalog.infrastructure.persistence.adapter.merchant;

import com.aionn.catalog.application.port.out.merchant.MerchantFollowerPersistencePort;
import com.aionn.contracts.port.catalog.MerchantFollowerQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MerchantFollowerJdbcAdapter implements MerchantFollowerPersistencePort, MerchantFollowerQueryPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void follow(String merchantId, String userId, Instant followedAt) {
        jdbcTemplate.update("""
                INSERT INTO merchant_followers (merchant_id, user_id, followed_at)
                VALUES (?, ?, ?)
                ON CONFLICT (merchant_id, user_id) DO NOTHING
                """, merchantId, userId, Timestamp.from(followedAt));
    }

    @Override
    public void unfollow(String merchantId, String userId) {
        jdbcTemplate.update("DELETE FROM merchant_followers WHERE merchant_id = ? AND user_id = ?",
                merchantId, userId);
    }

    @Override
    public boolean exists(String merchantId, String userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM merchant_followers WHERE merchant_id = ? AND user_id = ?
                """, Integer.class, merchantId, userId);
        return count != null && count > 0;
    }

    @Override
    public long countByMerchantId(String merchantId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM merchant_followers WHERE merchant_id = ?",
                Long.class, merchantId);
        return count == null ? 0L : count;
    }

    @Override
    public List<String> findUserIdsByMerchantId(String merchantId) {
        return jdbcTemplate.queryForList(
                "SELECT user_id FROM merchant_followers WHERE merchant_id = ?",
                String.class, merchantId);
    }

    @Override
    public List<String> findFollowerIds(String merchantId) {
        return findUserIdsByMerchantId(merchantId);
    }

    @Override
    public long countFollowers(String merchantId) {
        return countByMerchantId(merchantId);
    }

    @Override
    public boolean isFollowing(String merchantId, String userId) {
        return userId != null && exists(merchantId, userId);
    }
}
