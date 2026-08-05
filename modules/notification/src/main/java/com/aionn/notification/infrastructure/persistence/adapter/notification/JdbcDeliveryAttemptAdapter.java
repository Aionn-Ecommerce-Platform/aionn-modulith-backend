package com.aionn.notification.infrastructure.persistence.adapter.notification;

import com.aionn.notification.application.port.out.DeliveryAttemptPort;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JdbcDeliveryAttemptAdapter implements DeliveryAttemptPort {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public Attempt begin(String notificationId, NotificationChannel channel) {
        List<Attempt> unresolved = jdbc.query("""
                SELECT attempt_id, status FROM notification_delivery_attempts
                 WHERE notification_id = ? AND status IN ('STARTED', 'SUCCEEDED')
                 ORDER BY attempt_number DESC LIMIT 1
                """, (rs, row) -> new Attempt(rs.getString("attempt_id"),
                        Status.valueOf(rs.getString("status")), false), notificationId);
        if (!unresolved.isEmpty()) return unresolved.getFirst();

        Integer next = jdbc.queryForObject("""
                SELECT COALESCE(MAX(attempt_number), -1) + 1
                  FROM notification_delivery_attempts WHERE notification_id = ?
                """, Integer.class, notificationId);
        int attemptNumber = next == null ? 0 : next;
        String attemptId = notificationId + ":" + channel.name() + ":" + attemptNumber;
        jdbc.update("""
                INSERT INTO notification_delivery_attempts
                    (attempt_id, notification_id, channel, attempt_number, status)
                VALUES (?, ?, ?, ?, 'STARTED')
                """, attemptId, notificationId, channel.name(), attemptNumber);
        return new Attempt(attemptId, Status.STARTED, true);
    }

    @Override
    public void recordSucceeded(String attemptId, String providerMessageId) {
        jdbc.update("""
                UPDATE notification_delivery_attempts
                   SET status = 'SUCCEEDED', provider_message_id = ?, completed_at = NOW(), updated_at = NOW()
                 WHERE attempt_id = ?
                """, providerMessageId, attemptId);
    }

    @Override
    public void recordFailed(String attemptId, String error) {
        jdbc.update("""
                UPDATE notification_delivery_attempts
                   SET status = 'FAILED', error = ?, completed_at = NOW(), updated_at = NOW()
                 WHERE attempt_id = ?
                """, error, attemptId);
    }
}
