package com.aionn.sharedkernel.infrastructure.outbox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class OutboxEventRepository {

    private static final String CLAIM_SQL = """
            WITH candidates AS (
                SELECT candidate.event_id
                FROM outbox_events candidate
                WHERE (candidate.status = 'PENDING'
                       OR (candidate.status = 'PROCESSING' AND candidate.lease_until < NOW()))
                  AND candidate.next_attempt_at <= NOW()
                  AND NOT EXISTS (
                      SELECT 1 FROM outbox_events earlier
                      WHERE earlier.ordering_key = candidate.ordering_key
                        AND earlier.status IN ('PENDING', 'PROCESSING')
                        AND (earlier.occurred_at, earlier.event_id) < (candidate.occurred_at, candidate.event_id))
                ORDER BY candidate.occurred_at, candidate.event_id
                FOR UPDATE SKIP LOCKED
                LIMIT ?
            )
            UPDATE outbox_events event
            SET status = 'PROCESSING', lease_owner = ?, lease_until = ?,
                attempts = attempts + 1, updated_at = NOW()
            FROM candidates
            WHERE event.event_id = candidates.event_id
            RETURNING event.event_id, event.event_kind, event.event_type, event.payload_type,
                      event.payload::text, event.aggregate_type, event.aggregate_id,
                      event.occurred_at, event.attempts
            """;

    private final JdbcTemplate jdbcTemplate;

    OutboxEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<OutboxEventRecord> claim(int batchSize, String owner, Duration leaseDuration) {
        return jdbcTemplate.query(CLAIM_SQL, this::map,
                batchSize, owner, Timestamp.from(Instant.now().plus(leaseDuration)));
    }

    boolean markPublished(String eventId, String owner) {
        return jdbcTemplate.update("""
                UPDATE outbox_events SET status = 'PUBLISHED', published_at = NOW(),
                    lease_owner = NULL, lease_until = NULL, last_error = NULL, updated_at = NOW()
                WHERE event_id = ? AND status = 'PROCESSING' AND lease_owner = ?
                """, eventId, owner) == 1;
    }

    boolean wasProcessed(String consumerId, String eventId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_inbox WHERE consumer_id = ? AND event_id = ?",
                Integer.class, consumerId, eventId);
        return count != null && count > 0;
    }

    void markProcessed(String consumerId, String eventId) {
        jdbcTemplate.update("""
                INSERT INTO event_inbox (consumer_id, event_id)
                VALUES (?, ?)
                ON CONFLICT (consumer_id, event_id) DO NOTHING
                """, consumerId, eventId);
    }

    boolean markFailed(String eventId, String owner, String error, Instant nextAttempt,
            boolean deadLetter) {
        return jdbcTemplate.update("""
                UPDATE outbox_events SET status = ?, next_attempt_at = ?, last_error = ?,
                    lease_owner = NULL, lease_until = NULL, updated_at = NOW()
                WHERE event_id = ? AND status = 'PROCESSING' AND lease_owner = ?
                """, deadLetter ? "DEAD_LETTER" : "PENDING", Timestamp.from(nextAttempt),
                truncate(error), eventId, owner) == 1;
    }

    private OutboxEventRecord map(ResultSet result, int rowNumber) throws SQLException {
        return new OutboxEventRecord(result.getString("event_id"), result.getString("event_kind"),
                result.getString("event_type"), result.getString("payload_type"),
                result.getString("payload"), result.getString("aggregate_type"),
                result.getString("aggregate_id"), result.getTimestamp("occurred_at").toInstant(),
                result.getInt("attempts"));
    }

    private static String truncate(String error) {
        if (error == null || error.length() <= 4000) {
            return error;
        }
        return error.substring(0, 4000);
    }
}
