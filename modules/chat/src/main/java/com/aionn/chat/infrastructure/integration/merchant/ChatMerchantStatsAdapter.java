package com.aionn.chat.infrastructure.integration.merchant;

import com.aionn.contracts.port.chat.MerchantChatStatsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ChatMerchantStatsAdapter implements MerchantChatStatsPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public ResponseStats responseStats(String merchantId, Instant since) {
        if (merchantId == null || merchantId.isBlank() || since == null) {
            return new ResponseStats(0L, 0L, null);
        }
        return jdbcTemplate.queryForObject("""
                WITH buyer_first AS (
                    SELECT m.conversation_id, MIN(m.sent_at) AS first_buyer_at
                      FROM chat_messages m
                      JOIN chat_conversations c ON c.conversation_id = m.conversation_id
                     WHERE c.merchant_id = ?
                       AND m.sender_role = 'BUYER'
                       AND m.sent_at >= ?
                     GROUP BY m.conversation_id
                ),
                merchant_first AS (
                    SELECT m.conversation_id, MIN(m.sent_at) AS first_merchant_at
                      FROM chat_messages m
                      JOIN chat_conversations c ON c.conversation_id = m.conversation_id
                     WHERE c.merchant_id = ?
                       AND m.sender_role = 'MERCHANT'
                       AND m.sent_at >= ?
                     GROUP BY m.conversation_id
                )
                SELECT COUNT(*) AS conversations,
                       COUNT(mf.first_merchant_at) AS responded,
                       PERCENTILE_CONT(0.5) WITHIN GROUP (
                           ORDER BY EXTRACT(EPOCH FROM (mf.first_merchant_at - bf.first_buyer_at))
                       ) AS median_seconds
                  FROM buyer_first bf
                  LEFT JOIN merchant_first mf
                    ON mf.conversation_id = bf.conversation_id
                   AND mf.first_merchant_at >= bf.first_buyer_at
                """,
                (rs, rowNum) -> new ResponseStats(
                        rs.getLong("conversations"),
                        rs.getLong("responded"),
                        rs.getObject("median_seconds") == null
                                ? null
                                : Math.round(rs.getDouble("median_seconds"))),
                merchantId, Timestamp.from(since), merchantId, Timestamp.from(since));
    }
}
