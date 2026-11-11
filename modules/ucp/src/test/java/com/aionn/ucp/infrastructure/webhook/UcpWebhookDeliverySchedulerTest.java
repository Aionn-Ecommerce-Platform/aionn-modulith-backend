package com.aionn.ucp.infrastructure.webhook;

import com.aionn.ucp.application.port.out.UcpWebhookDispatcherPort;
import com.aionn.ucp.application.port.out.UcpWebhookEvent;
import com.aionn.ucp.infrastructure.config.UcpWebhookProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UcpWebhookDeliverySchedulerTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

    @Mock
    private UcpWebhookDeliveryStore store;
    @Mock
    private UcpWebhookDispatcherPort dispatcher;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private UcpWebhookDeliveryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new UcpWebhookDeliveryScheduler(store, dispatcher,
                new UcpWebhookProperties(5000, 50, 10), objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void marksDeliveredWhenDispatcherSucceeds() throws Exception {
        String payload = payload();
        when(store.findPending(50)).thenReturn(List.of(
                new UcpWebhookDeliveryStore.PendingDelivery("evt_1", "https://agent.example/hook", payload, 0)));
        when(dispatcher.dispatch(eq("https://agent.example/hook"), any())).thenReturn(true);

        scheduler.deliverPending();

        verify(store).markDelivered("evt_1");
    }

    @Test
    void schedulesRetryWhenDispatcherFails() throws Exception {
        String payload = payload();
        when(store.findPending(50)).thenReturn(List.of(
                new UcpWebhookDeliveryStore.PendingDelivery("evt_1", "https://agent.example/hook", payload, 2)));
        when(dispatcher.dispatch(eq("https://agent.example/hook"), any())).thenReturn(false);

        scheduler.deliverPending();

        ArgumentCaptor<Instant> nextAttempt = ArgumentCaptor.forClass(Instant.class);
        verify(store).markFailed(eq("evt_1"), eq(3), nextAttempt.capture(), any(), eq(false));
        assertThat(nextAttempt.getValue()).isAfter(NOW);
    }

    @Test
    void marksDeadAfterMaxAttempts() throws Exception {
        String payload = payload();
        when(store.findPending(50)).thenReturn(List.of(
                new UcpWebhookDeliveryStore.PendingDelivery("evt_1", "https://agent.example/hook", payload, 9)));
        when(dispatcher.dispatch(eq("https://agent.example/hook"), any())).thenReturn(false);

        scheduler.deliverPending();

        verify(store).markFailed(eq("evt_1"), eq(10), any(), any(), eq(true));
    }

    private String payload() throws Exception {
        return objectMapper.writeValueAsString(new UcpWebhookEvent(
                "evt_1", "order.placed", "ord_1", NOW, Map.of("status", "PLACED")));
    }
}
