package com.aionn.sharedkernel.infrastructure.outbox;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxDeadLetterServiceTest {

    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final OutboxDeadLetterService service = new OutboxDeadLetterService(repository);

    @Test
    void listsAValidatedBoundedPage() {
        var event = new OutboxDeadLetterService.DeadLetterEvent(
                "event-1", "INTEGRATION", "OrderCancelled", "Order", "order-1",
                Instant.parse("2026-08-05T00:00:00Z"), 8, "provider unavailable",
                Instant.parse("2026-08-05T00:05:00Z"));
        when(repository.findDeadLetters(20, 40)).thenReturn(List.of(event));
        when(repository.countDeadLetters()).thenReturn(41L);

        var page = service.list(2, 20);

        assertThat(page.content()).containsExactly(event);
        assertThat(page.totalElements()).isEqualTo(41);
        verify(repository).findDeadLetters(20, 40);
    }

    @Test
    void rejectsUnboundedPagination() {
        assertThatThrownBy(() -> service.list(-1, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.list(0, 101)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requeuesOnlyAnExistingDeadLetter() {
        when(repository.requeueDeadLetter("event-1")).thenReturn(true);
        service.requeue("event-1");
        verify(repository).requeueDeadLetter("event-1");

        when(repository.requeueDeadLetter("missing")).thenReturn(false);
        assertThatThrownBy(() -> service.requeue("missing"))
                .isInstanceOf(OutboxDeadLetterService.DeadLetterNotFoundException.class);
    }
}
