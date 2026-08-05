package com.aionn.sharedkernel.infrastructure.outbox;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxDeadLetterMetricsTest {

    @Test
    void exposesTheCurrentDeadLetterCount() {
        var meterRegistry = new SimpleMeterRegistry();
        var repository = mock(OutboxEventRepository.class);
        when(repository.countDeadLetters()).thenReturn(7L);

        new OutboxDeadLetterMetrics(meterRegistry, repository);

        assertThat(meterRegistry.get("aionn.outbox.dead.letter.count").gauge().value())
                .isEqualTo(7.0);
    }
}
