package com.aionn.ordering.infrastructure.compensation;

import com.aionn.ordering.application.port.out.CompensationTaskPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcCompensationTaskAdapter implements CompensationTaskPort {

    private final JdbcTemplate jdbc;

    @Override
    public void enqueue(Task task) {
        jdbc.update("""
                INSERT INTO ordering_compensation_tasks
                    (task_id, task_type, resource_id, user_id, order_id, reason)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (task_id) DO UPDATE SET
                    status = CASE WHEN ordering_compensation_tasks.status = 'COMPLETED'
                                  THEN 'COMPLETED' ELSE 'PENDING' END,
                    next_attempt_at = NOW(), updated_at = NOW()
                """, task.taskId(), task.type().name(), task.resourceId(), task.userId(), task.orderId(),
                task.reason());
    }

    @Override
    public Optional<Task> findById(String taskId) {
        return jdbc.query("""
                SELECT task_id, task_type, resource_id, user_id, order_id, reason, attempts
                  FROM ordering_compensation_tasks
                 WHERE task_id = ? AND status IN ('PENDING', 'FAILED')
                """, (rs, row) -> new Task(rs.getString("task_id"), Type.valueOf(rs.getString("task_type")),
                        rs.getString("resource_id"), rs.getString("user_id"), rs.getString("order_id"),
                        rs.getString("reason"), rs.getInt("attempts")), taskId).stream().findFirst();
    }

    @Override
    public List<String> findRetryableIds(Instant now, int maxAttempts, int limit) {
        return jdbc.queryForList("""
                SELECT task_id FROM ordering_compensation_tasks
                 WHERE status IN ('PENDING', 'FAILED') AND attempts < ? AND next_attempt_at <= ?
                 ORDER BY next_attempt_at, task_id LIMIT ?
                """, String.class, maxAttempts, Timestamp.from(now), limit);
    }

    @Override
    public void markCompleted(String taskId) {
        jdbc.update("""
                UPDATE ordering_compensation_tasks SET status = 'COMPLETED', last_error = NULL,
                    completed_at = NOW(), updated_at = NOW() WHERE task_id = ?
                """, taskId);
    }

    @Override
    public void markFailed(String taskId, String error, Instant nextAttemptAt, boolean deadLetter) {
        jdbc.update("""
                UPDATE ordering_compensation_tasks SET status = ?, attempts = attempts + 1,
                    last_error = ?, next_attempt_at = ?, updated_at = NOW() WHERE task_id = ?
                """, deadLetter ? "DEAD_LETTER" : "FAILED", error, Timestamp.from(nextAttemptAt), taskId);
    }

    @Override
    public long countDeadLetters() {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ordering_compensation_tasks WHERE status = 'DEAD_LETTER'", Long.class);
        return count == null ? 0 : count;
    }
}
