package com.aionn.sharedkernel.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class OutboxEventRepositoryTest {

    @Test
    @SuppressWarnings("unchecked")
    void claimsAndMapsOutboxRecords() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        OutboxEventRepository repository = new OutboxEventRepository(jdbc);
        repository.claim(5, "worker-1", Duration.ofMinutes(2));
        ArgumentCaptor<RowMapper<OutboxEventRecord>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        verify(jdbc).query(anyString(), mapperCaptor.capture(), eq(5), eq("worker-1"), any(Timestamp.class));

        ResultSet result = mock(ResultSet.class);
        Instant occurredAt = Instant.parse("2026-01-01T00:00:00Z");
        when(result.getString("event_id")).thenReturn("evt-1");
        when(result.getString("event_kind")).thenReturn("INTEGRATION");
        when(result.getString("event_type")).thenReturn("sample");
        when(result.getString("payload_type")).thenReturn("Sample");
        when(result.getString("payload")).thenReturn("{}");
        when(result.getString("aggregate_type")).thenReturn("Order");
        when(result.getString("aggregate_id")).thenReturn("order-1");
        when(result.getTimestamp("occurred_at")).thenReturn(Timestamp.from(occurredAt));
        when(result.getInt("attempts")).thenReturn(2);

        OutboxEventRecord record = mapperCaptor.getValue().mapRow(result, 0);
        assertEquals("evt-1", record.eventId());
        assertEquals(occurredAt, record.occurredAt());
        assertEquals(2, record.attempts());
    }

    @Test
    void updatesLifecycleAndInboxState() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        OutboxEventRepository repository = new OutboxEventRepository(jdbc);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("consumer"), eq("evt")))
                .thenReturn(1, 0, null);

        assertTrue(repository.wasProcessed("consumer", "evt"));
        assertFalse(repository.wasProcessed("consumer", "evt"));
        assertFalse(repository.wasProcessed("consumer", "evt"));
        repository.markProcessed("consumer", "evt");
        repository.markPublished("evt");
        repository.markFailed("evt", "temporary", Instant.parse("2026-01-01T00:01:00Z"), false);
        repository.markFailed("evt", "x".repeat(4100), Instant.parse("2026-01-01T00:02:00Z"), true);

        verify(jdbc).update(anyString(), eq("consumer"), eq("evt"));
        verify(jdbc).update(anyString(), eq("evt"));
        verify(jdbc).update(anyString(), eq("PENDING"), any(Timestamp.class), eq("temporary"), eq("evt"));
        verify(jdbc).update(anyString(), eq("DEAD_LETTER"), any(Timestamp.class), eq("x".repeat(4000)), eq("evt"));
    }
}
