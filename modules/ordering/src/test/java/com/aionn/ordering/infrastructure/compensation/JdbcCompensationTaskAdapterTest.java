package com.aionn.ordering.infrastructure.compensation;

import com.aionn.ordering.application.port.out.CompensationTaskPort.Task;
import com.aionn.ordering.application.port.out.CompensationTaskPort.Type;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcCompensationTaskAdapterTest {

    private static final Instant NOW = Instant.parse("2026-08-05T10:00:00Z");

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final JdbcCompensationTaskAdapter adapter = new JdbcCompensationTaskAdapter(jdbc);

    @Test
    void enqueueUpsertsTheTaskWithoutResurrectingACompletedRow() {
        adapter.enqueue(new Task("task-1", Type.RESERVATION_RELEASE, "res-1", "user-1", "order-1", "aborted", 0));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), eq("task-1"), eq("RESERVATION_RELEASE"), eq("res-1"),
                eq("user-1"), eq("order-1"), eq("aborted"));
        assertThat(sql.getValue())
                .contains("INSERT INTO ordering_compensation_tasks")
                .contains("ON CONFLICT (task_id) DO UPDATE")
                .contains("THEN 'COMPLETED' ELSE 'PENDING' END");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByIdMapsARetryableRow() throws Exception {
        ArgumentCaptor<RowMapper<Task>> mapper = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbc.query(anyString(), any(RowMapper.class), eq("task-1"))).thenReturn(List.of());

        adapter.findById("task-1");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), mapper.capture(), eq("task-1"));
        assertThat(sql.getValue()).contains("status IN ('PENDING', 'FAILED')");

        ResultSet row = mock(ResultSet.class);
        when(row.getString("task_id")).thenReturn("task-1");
        when(row.getString("task_type")).thenReturn("VOUCHER_RELEASE");
        when(row.getString("resource_id")).thenReturn("voucher-1");
        when(row.getString("user_id")).thenReturn("user-1");
        when(row.getString("order_id")).thenReturn("order-1");
        when(row.getString("reason")).thenReturn("aborted");
        when(row.getInt("attempts")).thenReturn(3);

        Task mapped = mapper.getValue().mapRow(row, 0);

        assertThat(mapped).isEqualTo(new Task("task-1", Type.VOUCHER_RELEASE, "voucher-1",
                "user-1", "order-1", "aborted", 3));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByIdReturnsEmptyWhenNoRowIsRetryable() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("missing"))).thenReturn(List.of());

        assertThat(adapter.findById("missing")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByIdReturnsTheFirstMatchingRow() {
        Task task = new Task("task-1", Type.RESERVATION_RELEASE, "res-1", "user-1", "order-1", "aborted", 1);
        when(jdbc.query(anyString(), any(RowMapper.class), eq("task-1"))).thenReturn(List.of(task));

        assertThat(adapter.findById("task-1")).contains(task);
    }

    @Test
    void findRetryableIdsBoundsAndOrdersTheClaim() {
        when(jdbc.queryForList(anyString(), eq(String.class), eq(10), any(Timestamp.class), eq(25)))
                .thenReturn(List.of("task-1", "task-2"));

        List<String> ids = adapter.findRetryableIds(NOW, 10, 25);

        assertThat(ids).containsExactly("task-1", "task-2");
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sql.capture(), eq(String.class), eq(10),
                eq(Timestamp.from(NOW)), eq(25));
        assertThat(sql.getValue())
                .contains("attempts < ?")
                .contains("next_attempt_at <= ?")
                .contains("ORDER BY next_attempt_at, task_id LIMIT ?");
    }

    @Test
    void markCompletedClearsTheLastError() {
        adapter.markCompleted("task-1");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), eq("task-1"));
        assertThat(sql.getValue())
                .contains("status = 'COMPLETED'")
                .contains("last_error = NULL")
                .contains("completed_at = NOW()");
    }

    @Test
    void markFailedIncrementsAttemptsAndKeepsTheTaskRetryable() {
        adapter.markFailed("task-1", "inventory unavailable", NOW.plusSeconds(8), false);

        verify(jdbc).update(anyString(), eq("FAILED"), eq("inventory unavailable"),
                eq(Timestamp.from(NOW.plusSeconds(8))), eq("task-1"));
    }

    @Test
    void markFailedDeadLettersWhenAttemptsAreExhausted() {
        adapter.markFailed("task-1", "still failing", NOW.plusSeconds(3600), true);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), eq("DEAD_LETTER"), eq("still failing"),
                eq(Timestamp.from(NOW.plusSeconds(3600))), eq("task-1"));
        assertThat(sql.getValue()).contains("attempts = attempts + 1");
    }

    @Test
    void countDeadLettersReturnsTheStoredCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(4L);

        assertThat(adapter.countDeadLetters()).isEqualTo(4L);
    }

    @Test
    void countDeadLettersTreatsAMissingCountAsZero() {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

        assertThat(adapter.countDeadLetters()).isZero();
    }
}
